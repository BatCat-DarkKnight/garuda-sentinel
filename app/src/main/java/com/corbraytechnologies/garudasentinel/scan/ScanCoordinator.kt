/*
 * Garuda Sentinel, a personal Android privacy tool.
 * Copyright (C) 2025-2026 Karl Corbray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.corbraytechnologies.garudasentinel.scan

import androidx.core.net.toUri
import com.corbraytechnologies.garudasentinel.collect.AppCollector
import com.corbraytechnologies.garudasentinel.collect.FileCollector
import com.corbraytechnologies.garudasentinel.collect.MediaCollector
import com.corbraytechnologies.garudasentinel.collect.UsageCollector
import com.corbraytechnologies.garudasentinel.data.AppDatabase
import com.corbraytechnologies.garudasentinel.collect.WatcherCollector
import com.corbraytechnologies.garudasentinel.data.ScanSnapshotEntity
import com.corbraytechnologies.garudasentinel.model.ScanSnapshot
import com.corbraytechnologies.garudasentinel.model.SnapshotApp
import com.corbraytechnologies.garudasentinel.model.SnapshotWatchers
import com.corbraytechnologies.garudasentinel.utils.SensitivePermissions
import kotlinx.serialization.json.Json
import com.corbraytechnologies.garudasentinel.data.ScanLogEntity
import com.corbraytechnologies.garudasentinel.data.SettingsStore
import com.corbraytechnologies.garudasentinel.model.UsageSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections

enum class ScanStep(val label: String) {
    APPS("Installed apps"),
    MEDIA("Photos, videos and audio"),
    FILES("Chosen folder"),
    USAGE("App usage"),
}

sealed interface StepStatus {
    data object Waiting : StepStatus
    data object Running : StepStatus
    data class Done(val count: Int) : StepStatus
    data class Skipped(val reason: String) : StepStatus
    data class Failed(val message: String) : StepStatus
}

data class ScanState(
    val running: Boolean = false,
    val steps: Map<ScanStep, StepStatus> = emptyMap(),
    val lastLog: ScanLogEntity? = null,
)

/**
 * Runs one full scan at a time on the application scope, so leaving a screen or
 * rotating the device does not cancel it. Results go to Room; the UI observes Room.
 */
class ScanCoordinator(
    private val scope: CoroutineScope,
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val apps: AppCollector,
    private val media: MediaCollector,
    private val files: FileCollector,
    private val usage: UsageCollector,
    private val watchers: WatcherCollector,
) {
    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val _usage = MutableStateFlow<UsageSummary?>(null)

    /** Latest usage snapshot. Kept in memory only, and refreshed on every scan. */
    val usageSummary: StateFlow<UsageSummary?> = _usage.asStateFlow()

    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch { runScan() }
    }

    fun cancel() {
        job?.cancel()
    }

    suspend fun refreshUsage(): UsageSummary? = usage.collect().also { _usage.value = it }

    /** Forget in-memory results, used after the user deletes all data. */
    fun clearMemory() {
        cancel()
        _usage.value = null
        _state.value = ScanState()
    }

    private suspend fun runScan() {
        val startedAt = System.currentTimeMillis()
        _state.update { ScanState(running = true, steps = ScanStep.entries.associateWith { StepStatus.Waiting }, lastLog = it.lastLog) }
        // Steps run in parallel, so notes are collected in a thread-safe list.
        val notes: MutableList<String> = Collections.synchronizedList(mutableListOf())
        try {
            // Child coroutines of the scan job, so cancel() stops every step.
            val counts = coroutineScope {
                val appsJob = async { step(ScanStep.APPS) { apps.collect().also { db.appDao().replaceAll(it) }.size } }
                val mediaJob = async {
                    step(ScanStep.MEDIA) {
                        val result = media.collect()
                        if (!result.permitted) throw SkipException("Media permission not granted.")
                        notes += result.notes
                        db.mediaDao().replaceAll(result.items)
                        result.items.size
                    }
                }
                val filesJob = async {
                    val tree = settings.filesTreeUri.first()
                    if (tree == null) {
                        skip(ScanStep.FILES, "No folder chosen yet.")
                    } else {
                        step(ScanStep.FILES) {
                            val result = files.collect(tree.toUri())
                            notes += result.notes
                            db.fileDao().replaceAll(result.items)
                            result.items.size
                        }
                    }
                }
                val usageJob = async {
                    step(ScanStep.USAGE) {
                        refreshUsage()?.apps?.size ?: throw SkipException("Usage access not granted.")
                    }
                }
                listOf(appsJob, mediaJob, filesJob, usageJob).map { it.await() }
            }

            _state.value.steps.forEach { (s, status) ->
                when (status) {
                    is StepStatus.Failed -> notes += "${s.label} failed: ${status.message}"
                    is StepStatus.Skipped -> notes += "${s.label} skipped: ${status.reason}"
                    else -> Unit
                }
            }
            val log = ScanLogEntity(
                startedAt = startedAt,
                finishedAt = System.currentTimeMillis(),
                appsCount = counts[0],
                mediaCount = counts[1],
                filesCount = counts[2],
                usageCount = counts[3],
                notes = notes.distinct().joinToString("\n"),
            )
            val id = db.scanLogDao().insert(log)
            saveSnapshot(log.finishedAt)
            _state.update { it.copy(running = false, lastLog = log.copy(id = id)) }
        } catch (e: CancellationException) {
            _state.update { it.copy(running = false) }
            throw e
        }
    }

    /**
     * Records what the phone looked like after this scan, so the next scan can say what changed.
     * Failures here never fail the scan.
     */
    private suspend fun saveSnapshot(takenAt: Long) {
        runCatching {
            val apps = db.appDao().getAll()
            val signals = watchers.collect(apps)
            val snapshot = ScanSnapshot(
                takenAt = takenAt,
                apps = apps.map { app ->
                    SnapshotApp(
                        packageName = app.packageName,
                        appName = app.appName,
                        installer = app.installer,
                        sensitivePermissions = app.grantedPermissionList.filter { it in SensitivePermissions.all },
                    )
                },
                watchers = SnapshotWatchers(
                    accessibilityServices = signals.accessibilityServices.map { it.packageName },
                    notificationListeners = signals.notificationListeners.map { it.packageName },
                    deviceAdmins = signals.deviceAdmins.map { it.packageName },
                    backgroundLocationApps = signals.backgroundLocationApps.map { it.packageName },
                    appsWithoutLauncherIcon = signals.appsWithoutLauncherIcon.map { it.packageName },
                    userCaCertificateCount = signals.userCaCertificates.size,
                    usbDebuggingEnabled = signals.usbDebuggingEnabled,
                    screenLockEnabled = signals.screenLockEnabled,
                ),
            )
            db.scanSnapshotDao().add(
                ScanSnapshotEntity(takenAt = takenAt, payload = snapshotJson.encodeToString(ScanSnapshot.serializer(), snapshot))
            )
        }
    }

    companion object {
        /** Used for the snapshot payload column, in both directions. */
        val snapshotJson = Json { ignoreUnknownKeys = true }
    }

    private class SkipException(message: String) : Exception(message)

    private fun skip(step: ScanStep, reason: String): Int {
        setStep(step, StepStatus.Skipped(reason))
        return 0
    }

    /** Runs one step and records its status. Returns the item count, or 0 when it did not complete. */
    private suspend fun step(step: ScanStep, block: suspend () -> Int): Int {
        setStep(step, StepStatus.Running)
        return try {
            block().also { setStep(step, StepStatus.Done(it)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SkipException) {
            skip(step, e.message.orEmpty())
        } catch (e: Exception) {
            setStep(step, StepStatus.Failed(e.message ?: e.javaClass.simpleName))
            0
        }
    }

    private fun setStep(step: ScanStep, status: StepStatus) {
        _state.update { it.copy(steps = it.steps + (step to status)) }
    }
}
