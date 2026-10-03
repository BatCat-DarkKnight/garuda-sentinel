package com.corbraytechnologies.garudasentinel.ui

import androidx.core.net.toUri
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.corbraytechnologies.garudasentinel.AppContainer
import com.corbraytechnologies.garudasentinel.GarudaApp
import com.corbraytechnologies.garudasentinel.collect.LocationCollector
import com.corbraytechnologies.garudasentinel.data.AppMetadataEntity
import com.corbraytechnologies.garudasentinel.data.FileMetadataEntity
import com.corbraytechnologies.garudasentinel.data.LocationMetadataEntity
import com.corbraytechnologies.garudasentinel.data.MediaMetadataEntity
import com.corbraytechnologies.garudasentinel.data.ScanLogEntity
import com.corbraytechnologies.garudasentinel.data.StorageMode
import com.corbraytechnologies.garudasentinel.export.ExportCategory
import com.corbraytechnologies.garudasentinel.model.BatteryInfo
import com.corbraytechnologies.garudasentinel.model.DeviceInfo
import com.corbraytechnologies.garudasentinel.model.InputDetails
import com.corbraytechnologies.garudasentinel.model.NetworkDetails
import com.corbraytechnologies.garudasentinel.model.UsageSummary
import com.corbraytechnologies.garudasentinel.model.WatcherSignals
import com.corbraytechnologies.garudasentinel.model.ScanSnapshot
import com.corbraytechnologies.garudasentinel.scan.ScanCoordinator
import com.corbraytechnologies.garudasentinel.scan.ScanState
import com.corbraytechnologies.garudasentinel.utils.ScanChanges
import com.corbraytechnologies.garudasentinel.utils.ScanDiff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Creates a view model backed by the application container. */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(crossinline create: (AppContainer) -> VM): VM =
    viewModel(factory = viewModelFactory {
        initializer { create((this[APPLICATION_KEY] as GarudaApp).container) }
    })

private fun <T> Flow<T>.stateIn(vm: ViewModel, initial: T): StateFlow<T> =
    stateIn(vm.viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

/** App-wide state shared by the navigation host: EULA, scan progress and collected data. */
class MainViewModel(private val c: AppContainer) : ViewModel() {
    /** null while loading, so the first screen is not chosen before the setting is read. */
    val eulaAccepted: StateFlow<Boolean?> = c.settings.eulaAccepted.stateIn(this, null)
    val scanState: StateFlow<ScanState> = c.scanCoordinator.state
    val usage: StateFlow<UsageSummary?> = c.scanCoordinator.usageSummary

    val apps: StateFlow<List<AppMetadataEntity>> = c.db.appDao().observeAll().stateIn(this, emptyList())
    val media: StateFlow<List<MediaMetadataEntity>> = c.db.mediaDao().observeAll().stateIn(this, emptyList())
    val files: StateFlow<List<FileMetadataEntity>> = c.db.fileDao().observeAll().stateIn(this, emptyList())
    val scanLogs: StateFlow<List<ScanLogEntity>> = c.db.scanLogDao().observeAll().stateIn(this, emptyList())

    /** What changed between the last two scans, or null after the first scan. */
    val changes: StateFlow<ScanChanges?> = c.db.scanSnapshotDao().observeLatest(2)
        .map { rows ->
            val parsed = rows.mapNotNull { row ->
                runCatching { ScanCoordinator.snapshotJson.decodeFromString(ScanSnapshot.serializer(), row.payload) }.getOrNull()
            }
            if (parsed.size < 2) null else ScanDiff.compute(previous = parsed[1], current = parsed[0])
        }
        .stateIn(this, null)

    fun acceptEula() = viewModelScope.launch { c.settings.setEulaAccepted(true) }
    fun startScan() = c.scanCoordinator.start()
    fun cancelScan() = c.scanCoordinator.cancel()
    fun refreshUsage() = viewModelScope.launch { c.scanCoordinator.refreshUsage() }
}

class FilesViewModel(private val c: AppContainer) : ViewModel() {
    val treeUri: StateFlow<String?> = c.settings.filesTreeUri.stateIn(this, null)

    /** Keeps read access to the folder across restarts, then rescans. */
    fun onFolderPicked(context: Context, uri: Uri) = viewModelScope.launch {
        val resolver = context.applicationContext.contentResolver
        treeUri.value?.let { old ->
            runCatching { resolver.releasePersistableUriPermission(old.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        c.settings.setFilesTreeUri(uri.toString())
        c.scanCoordinator.start()
    }

    fun forgetFolder(context: Context) = viewModelScope.launch {
        treeUri.value?.let { old ->
            runCatching {
                context.applicationContext.contentResolver
                    .releasePersistableUriPermission(old.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        c.settings.setFilesTreeUri(null)
        c.db.fileDao().clear()
    }
}

data class DeviceSnapshot(
    val device: DeviceInfo,
    val battery: BatteryInfo,
    val network: NetworkDetails,
    val input: InputDetails,
)

class DeviceViewModel(private val c: AppContainer) : ViewModel() {
    private val _snapshot = MutableStateFlow<DeviceSnapshot?>(null)
    val snapshot: StateFlow<DeviceSnapshot?> = _snapshot.asStateFlow()

    val locations: StateFlow<List<LocationMetadataEntity>> = c.db.locationDao().observeAll().stateIn(this, emptyList())

    private val _locationStatus = MutableStateFlow<String?>(null)
    val locationStatus: StateFlow<String?> = _locationStatus.asStateFlow()

    private val _readingLocation = MutableStateFlow(false)
    val readingLocation: StateFlow<Boolean> = _readingLocation.asStateFlow()

    fun refresh() = viewModelScope.launch {
        _snapshot.value = withContext(Dispatchers.IO) {
            val d = c.deviceCollector
            DeviceSnapshot(d.device(), d.battery(), d.network(), d.input())
        }
    }

    fun readLocation() = viewModelScope.launch {
        if (_readingLocation.value) return@launch
        _readingLocation.value = true
        _locationStatus.value = "Reading location..."
        when (val outcome = c.locationCollector.readOnce()) {
            is LocationCollector.Outcome.Success -> {
                c.db.locationDao().insert(outcome.location)
                _locationStatus.value = null
            }
            is LocationCollector.Outcome.Unavailable -> _locationStatus.value = outcome.reason
        }
        _readingLocation.value = false
    }

    fun clearLocations() = viewModelScope.launch { c.db.locationDao().clear() }
}

class HistoryViewModel(private val c: AppContainer) : ViewModel() {
    val logs: StateFlow<List<ScanLogEntity>> = c.db.scanLogDao().observeAll().stateIn(this, emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val selection = MutableStateFlow(ExportCategory.entries.filter { it.onByDefault }.toSet())

    /** Photo GPS is left out of exports unless the user opts in. */
    val includePhotoLocations = MutableStateFlow(false)

    /** True when this process keeps scan results in memory only. */
    val memoryOnly: Boolean = c.memoryOnly

    /** Opt-in FLAG_SECURE, applied by MainActivity. */
    val blockScreenshots: StateFlow<Boolean> = c.settings.blockScreenshots.stateIn(this, false)

    fun setBlockScreenshots(blocked: Boolean) = viewModelScope.launch { c.settings.setBlockScreenshots(blocked) }

    fun toggle(category: ExportCategory) {
        selection.value = selection.value.let { if (category in it) it - category else it + category }
    }

    fun export(target: Uri, password: CharArray? = null) = viewModelScope.launch {
        _busy.value = true
        _message.value = try {
            val bytes = c.exportService.export(target, selection.value, includePhotoLocations.value, password)
            "Export saved (${com.corbraytechnologies.garudasentinel.utils.formatFileSize(bytes.toLong())})."
        } catch (e: Exception) {
            "Export failed: ${e.message ?: e.javaClass.simpleName}"
        }
        _busy.value = false
    }

    fun deleteLog(id: Long) = viewModelScope.launch { c.db.scanLogDao().delete(id) }

    /**
     * Turns "Forget results when I close the app" on or off. Current results are deleted either way (on disk
     * when turning it on, in memory when turning it off), then [restart] reopens the app so the
     * database is recreated in the chosen mode and no scan is written to the wrong place.
     */
    fun setMemoryOnly(on: Boolean, restart: () -> Unit) = viewModelScope.launch {
        _busy.value = true
        c.scanCoordinator.clearMemory()
        withContext(Dispatchers.IO) {
            c.db.clearAllTables()
            StorageMode.setMemoryOnly(c.context, on)
        }
        restart()
    }

    fun deleteAll() = viewModelScope.launch {
        _busy.value = true
        c.scanCoordinator.clearMemory()
        c.dataWiper.deleteAll()
        _busy.value = false
        _message.value = "All scan data on this device was deleted."
    }

    fun clearMessage() {
        _message.value = null
    }
}

/** Reads the watcher signals on demand from the apps found by the last scan. */
class WatchersViewModel(private val c: AppContainer) : ViewModel() {
    private val _signals = MutableStateFlow<WatcherSignals?>(null)
    val signals: StateFlow<WatcherSignals?> = _signals.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun refresh() = viewModelScope.launch {
        _busy.value = true
        val apps = withContext(Dispatchers.IO) { c.db.appDao().getAll() }
        _signals.value = c.watcherCollector.collect(apps)
        _busy.value = false
    }

    fun appSettingsIntent(packageName: String) = c.watcherCollector.appSettingsIntent(packageName)
}

