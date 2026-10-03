package com.corbraytechnologies.garudasentinel.findings

import com.corbraytechnologies.garudasentinel.data.AppMetadataEntity
import com.corbraytechnologies.garudasentinel.data.MediaMetadataEntity
import com.corbraytechnologies.garudasentinel.model.WatcherApp
import com.corbraytechnologies.garudasentinel.model.WatcherFinding
import com.corbraytechnologies.garudasentinel.model.WatcherKind
import com.corbraytechnologies.garudasentinel.model.WatcherLevel
import com.corbraytechnologies.garudasentinel.model.WatcherSignals
import com.corbraytechnologies.garudasentinel.utils.AppLists
import com.corbraytechnologies.garudasentinel.utils.WatcherRules

/** What the rules read. All of it comes from the last scan and the watcher signals; nothing new is collected. */
data class ReportInput(
    val apps: List<AppMetadataEntity>,
    val ownPackage: String,
    val media: List<MediaMetadataEntity>,
    val signals: WatcherSignals,
    /** False when the app has no access to photos, so the photo check could not run. */
    val photosReadable: Boolean = true,
    /** False when photo location access is off, so Android hid the GPS inside photos. */
    val photoLocationsReadable: Boolean = true,
)

/**
 * Turns scan results into the ranked findings on the Report screen. Pure, so every rule is
 * unit tested without a device. App-based rules count only apps the user installed, and leave
 * out Garuda Sentinel itself, the same way the Apps screen does.
 */
object FindingRules {

    const val RECORD_AUDIO = "android.permission.RECORD_AUDIO"
    const val CAMERA = "android.permission.CAMERA"
    const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"

    /** Media type of photos, as stored by MediaCollector. */
    private const val TYPE_IMAGE = "image"

    fun build(input: ReportInput): Report {
        val byPackage = input.apps.associateBy { it.packageName }
        val watchers = WatcherRules.findings(input.signals)
        fun isUserApp(app: WatcherApp) = !app.isSystemApp && app.packageName != input.ownPackage
        fun recencyOf(packages: Collection<String>) = packages.maxOfOrNull { byPackage[it]?.lastUpdateTime ?: 0L } ?: 0L

        val findings = mutableListOf<Finding>()

        // HIGH: a sideloaded app that holds notification access, an accessibility service or device admin.
        val coveredBySideload = mutableMapOf<WatcherKind, MutableSet<String>>()
        val sideloadedFlagged = mutableSetOf<String>()
        input.signals.installedFromOutsideAStore.filter(::isUserApp).forEach { app ->
            val kinds = buildList {
                if (input.signals.accessibilityServices.any { it.packageName == app.packageName }) add(WatcherKind.ACCESSIBILITY)
                if (input.signals.deviceAdmins.any { it.packageName == app.packageName }) add(WatcherKind.DEVICE_ADMIN)
                if (input.signals.notificationListeners.any { it.packageName == app.packageName }) add(WatcherKind.NOTIFICATIONS)
            }
            if (kinds.isEmpty()) return@forEach
            kinds.forEach { coveredBySideload.getOrPut(it) { mutableSetOf() } += app.packageName }
            sideloadedFlagged += app.packageName
            findings += Finding(
                severity = FindingSeverity.HIGH,
                title = "An app from outside a store can " + when (kinds.first()) {
                    WatcherKind.ACCESSIBILITY -> "control your screen"
                    WatcherKind.DEVICE_ADMIN -> "manage this phone"
                    else -> "read your notifications"
                },
                sentence = "${app.appName} came from outside an app store and has " +
                    Wording.list(kinds.map { capability(it) }) + ".",
                action = FindingAction.ReviewApp(app.packageName),
                recency = recencyOf(listOf(app.packageName)),
            )
        }

        // HIGH: every other "needs attention" watcher, such as a certificate added by hand.
        watchers.filter { it.level == WatcherLevel.ATTENTION }.forEach { watcher ->
            val covered = coveredBySideload[watcher.kind].orEmpty()
            val apps = watcher.apps.filter { it.packageName !in covered }
            if (watcher.apps.isNotEmpty() && apps.isEmpty()) return@forEach
            findings += Finding(
                severity = FindingSeverity.HIGH,
                title = watcher.title,
                sentence = attentionSentence(watcher.kind, apps, watcher.explanation),
                action = FindingAction.OpenSetting(settingsTarget(watcher.kind) ?: SettingsTarget.SECURITY),
                recency = recencyOf(apps.map { it.packageName }),
            )
        }

        // MEDIUM: photos that carry GPS coordinates. Skipped when photos could not be read, so stale
        // results from an earlier scan do not sit next to a "not checked" row.
        val located = if (input.photosReadable) {
            input.media.filter { it.type == TYPE_IMAGE && it.gpsLat != null && it.gpsLong != null }
        } else {
            emptyList()
        }
        if (located.isNotEmpty()) {
            val n = located.size
            findings += Finding(
                severity = FindingSeverity.MEDIUM,
                title = if (n == 1) "1 photo shows exactly where it was taken" else "$n photos show exactly where they were taken",
                sentence = "Anyone you send the original " + (if (n == 1) "file" else "files") + " to can read the location inside.",
                action = FindingAction.SeePlaces,
                recency = located.maxOf { it.dateTaken ?: it.lastModified },
            )
        }

        // MEDIUM: apps the user installed that can use the microphone, camera or precise location.
        val userApps = AppLists.userInstalled(input.apps, input.ownPackage)
        listOf(
            RECORD_AUDIO to "your microphone",
            CAMERA to "your camera",
            FINE_LOCATION to "your precise location",
        ).forEach { (permission, what) ->
            val holders = userApps.filter { permission in it.grantedPermissionList }
            if (holders.isEmpty()) return@forEach
            val n = holders.size
            findings += Finding(
                severity = FindingSeverity.MEDIUM,
                title = (if (n == 1) "1 app you installed can use " else "$n apps you installed can use ") + what,
                sentence = holdersSentence(holders.map { it.appName }, "this permission"),
                action = FindingAction.SeeAppsByAccess,
                recency = holders.maxOf { it.lastUpdateTime },
            )
        }

        // MEDIUM: background location granted to an app the user installed.
        val background = input.signals.backgroundLocationApps.filter(::isUserApp)
        if (background.isNotEmpty()) {
            val n = background.size
            findings += Finding(
                severity = FindingSeverity.MEDIUM,
                title = if (n == 1) "1 app can see where you are while closed" else "$n apps can see where you are while closed",
                sentence = holdersSentence(background.map { it.appName }, "background location"),
                action = FindingAction.SeeAppsByAccess,
                recency = recencyOf(background.map { it.packageName }),
            )
        }

        // LOW: USB debugging.
        if (input.signals.usbDebuggingEnabled) {
            findings += Finding(
                severity = FindingSeverity.LOW,
                title = "USB debugging is on",
                sentence = "A computer connected by cable can read and control much of the phone.",
                action = FindingAction.OpenSetting(SettingsTarget.DEVELOPER_OPTIONS, "Open developer options"),
            )
        }

        // LOW: every other "worth knowing" watcher. Background location and USB debugging are covered above.
        watchers.filter { it.level == WatcherLevel.CHECK }.forEach { watcher ->
            when (watcher.kind) {
                WatcherKind.BACKGROUND_LOCATION -> return@forEach
                WatcherKind.DEVELOPER -> if (input.signals.usbDebuggingEnabled) return@forEach
                else -> Unit
            }
            val apps = watcher.apps.filter { isUserApp(it) && it.packageName !in sideloadedFlagged }
            if (watcher.apps.isNotEmpty() && apps.isEmpty()) return@forEach
            findings += Finding(
                severity = FindingSeverity.LOW,
                title = watcher.title,
                sentence = checkSentence(watcher.kind, apps, watcher.explanation),
                action = apps.singleOrNull()?.let { FindingAction.OpenAppInfo(it.packageName) }
                    ?: FindingAction.OpenSetting(settingsTarget(watcher.kind) ?: SettingsTarget.ALL_APPS),
                recency = recencyOf(apps.map { it.packageName }),
            )
        }

        return Report(
            findings = rank(findings),
            passed = passedCount(watchers),
            notChecked = notChecked(input),
        )
    }

    /** High, then medium, then low; within a severity the most recent first. */
    fun rank(findings: List<Finding>): List<Finding> =
        findings.sortedWith(compareBy<Finding> { it.severity.ordinal }.thenByDescending { it.recency })

    /**
     * Every "all clear" watcher counts as passed. "Not checked on this phone" is left out: a check
     * that could not run did not pass.
     */
    fun passedCount(watchers: List<WatcherFinding>): Int =
        watchers.count { it.level == WatcherLevel.FINE && it.kind != WatcherKind.NOT_CHECKED }

    /** The settings screen behind a watcher, or null when there is none to open. */
    fun settingsTarget(kind: WatcherKind): SettingsTarget? = when (kind) {
        WatcherKind.ACCESSIBILITY, WatcherKind.SYSTEM_ACCESSIBILITY -> SettingsTarget.ACCESSIBILITY
        WatcherKind.NOTIFICATIONS -> SettingsTarget.NOTIFICATION_ACCESS
        WatcherKind.DEVICE_ADMIN, WatcherKind.CERTIFICATES, WatcherKind.NO_SCREEN_LOCK, WatcherKind.SCREEN_LOCK_ON -> SettingsTarget.SECURITY
        WatcherKind.BACKGROUND_LOCATION -> SettingsTarget.LOCATION
        WatcherKind.HIDDEN_APPS, WatcherKind.SIDELOADED -> SettingsTarget.ALL_APPS
        WatcherKind.DEVELOPER -> SettingsTarget.DEVELOPER_OPTIONS
        WatcherKind.DEFAULT_SMS -> SettingsTarget.DEFAULT_APPS
        WatcherKind.NOT_CHECKED -> null
    }

    private fun notChecked(input: ReportInput): List<NotChecked> = buildList {
        if (!input.photosReadable) {
            add(
                NotChecked(
                    "Photos were not checked",
                    "Garuda Sentinel has no access to your photos, so it could not look for locations in them.",
                    FindingAction.SetUp,
                )
            )
        } else if (!input.photoLocationsReadable) {
            add(
                NotChecked(
                    "Photo locations were not checked",
                    "Android hides the location inside photos until you allow photo location access.",
                    FindingAction.SetUp,
                )
            )
        }
        val unread = input.signals.unavailable.size
        if (unread > 0) {
            add(
                NotChecked(
                    if (unread == 1) "1 setting could not be read" else "$unread settings could not be read",
                    "This phone did not let the app read " + (if (unread == 1) "it." else "them."),
                    FindingAction.SeeWhoCanWatch,
                )
            )
        }
    }

    private fun capability(kind: WatcherKind): String = when (kind) {
        WatcherKind.ACCESSIBILITY -> "an accessibility service turned on"
        WatcherKind.DEVICE_ADMIN -> "device administrator rights"
        else -> "notification access"
    }

    private fun attentionSentence(kind: WatcherKind, apps: List<WatcherApp>, fallback: String): String {
        val who = Wording.subject(apps.map { it.appName }, "app")
        return when (kind) {
            WatcherKind.ACCESSIBILITY -> "$who can see what is on screen and what you type."
            WatcherKind.NOTIFICATIONS -> "$who can see every notification, including message previews and codes."
            WatcherKind.DEVICE_ADMIN -> "$who can lock or erase this phone."
            WatcherKind.CERTIFICATES -> Wording.subject(apps.map { it.appName }, "certificate") +
                ", added by hand, can let whoever issued " + (if (apps.size == 1) "it" else "them") +
                " read traffic from some apps."
            else -> fallback
        }
    }

    private fun checkSentence(kind: WatcherKind, apps: List<WatcherApp>, fallback: String): String {
        val who = Wording.subject(apps.map { it.appName }, "app")
        val plural = apps.size != 1
        return when (kind) {
            WatcherKind.HIDDEN_APPS -> "$who " + (if (plural) "have" else "has") + " no icon in the app list."
            WatcherKind.SIDELOADED -> "$who came from a file or another app, not a store."
            WatcherKind.DEVELOPER -> "On its own this changes little, but it allows USB debugging."
            else -> fallback
        }
    }

    /** Names up to three apps; beyond that, asks the user to check each one. */
    private fun holdersSentence(names: List<String>, what: String): String =
        if (names.size <= Wording.MAX_NAMES) {
            Wording.list(names) + " " + (if (names.size == 1) "has" else "have") + " $what right now."
        } else {
            "Check that each one still needs it."
        }
}
