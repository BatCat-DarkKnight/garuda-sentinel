package com.corbraytechnologies.garudasentinel.utils

import com.corbraytechnologies.garudasentinel.model.WatcherApp
import com.corbraytechnologies.garudasentinel.model.WatcherFinding
import com.corbraytechnologies.garudasentinel.model.WatcherKind
import com.corbraytechnologies.garudasentinel.model.WatcherLevel
import com.corbraytechnologies.garudasentinel.model.WatcherSignals

/**
 * Turns raw watcher signals into plain-language findings. Pure, so the judgement can be tested
 * without a device. Nothing here changes a setting; the app only reads.
 */
object WatcherRules {

    fun findings(signals: WatcherSignals): List<WatcherFinding> = buildList {
        val accessibility = signals.accessibilityServices.filterNot { it.isSystemApp }
        if (accessibility.isNotEmpty()) {
            add(
                WatcherFinding(
                    WatcherLevel.ATTENTION,
                    "Apps that can read your screen",
                    "An accessibility service can see everything on screen and what you type. Some apps need it, " +
                        "for example screen readers and password managers. Anything you do not recognise is worth checking.",
                    accessibility,
                    WatcherKind.ACCESSIBILITY,
                )
            )
        }
        val systemAccessibility = signals.accessibilityServices.filter { it.isSystemApp }
        if (systemAccessibility.isNotEmpty()) {
            add(
                WatcherFinding(
                    WatcherLevel.FINE,
                    "Accessibility services from your phone maker",
                    "These come with the phone or its system apps.",
                    systemAccessibility,
                    WatcherKind.SYSTEM_ACCESSIBILITY,
                )
            )
        }
        val listeners = signals.notificationListeners.filterNot { it.isSystemApp }
        if (listeners.isNotEmpty()) {
            add(
                WatcherFinding(
                    WatcherLevel.ATTENTION,
                    "Apps that can read your notifications",
                    "These apps see every notification, including message previews and one-time codes. " +
                        "Smart watches and car apps normally need this.",
                    listeners,
                    WatcherKind.NOTIFICATIONS,
                )
            )
        }
        val admins = signals.deviceAdmins.filterNot { it.isSystemApp }
        if (admins.isNotEmpty()) {
            add(
                WatcherFinding(
                    WatcherLevel.ATTENTION,
                    "Apps that control the phone",
                    "A device administrator can lock the phone, change the password or erase it. Work profiles " +
                        "and device finders use this.",
                    admins,
                    WatcherKind.DEVICE_ADMIN,
                )
            )
        }
        if (signals.userCaCertificates.isNotEmpty()) {
            add(
                WatcherFinding(
                    WatcherLevel.ATTENTION,
                    "Certificates added by someone",
                    "A certificate installed by hand lets whoever issued it read traffic from some apps. " +
                        "Company phones often have one. Otherwise it should not be there.",
                    signals.userCaCertificates,
                    WatcherKind.CERTIFICATES,
                )
            )
        }
        if (!signals.screenLockEnabled) {
            add(
                WatcherFinding(
                    WatcherLevel.ATTENTION,
                    "No screen lock",
                    "Anyone holding the phone can open it and see everything, including this app.",
                    kind = WatcherKind.NO_SCREEN_LOCK,
                )
            )
        }
        val background = signals.backgroundLocationApps.filterNot { it.isSystemApp }
        if (background.isNotEmpty()) {
            add(
                WatcherFinding(
                    WatcherLevel.CHECK,
                    "Apps that can follow your location in the background",
                    "These apps can read your location even when you are not using them.",
                    background,
                    WatcherKind.BACKGROUND_LOCATION,
                )
            )
        }
        val hidden = signals.appsWithoutLauncherIcon.filterNot { it.isSystemApp }
        if (hidden.isNotEmpty()) {
            add(
                WatcherFinding(
                    WatcherLevel.CHECK,
                    "Apps with no icon in the app list",
                    "An app with no icon does not show on the home screen. Plug-ins and keyboards are often like " +
                        "this, and so is monitoring software.",
                    hidden,
                    WatcherKind.HIDDEN_APPS,
                )
            )
        }
        val sideloaded = signals.installedFromOutsideAStore.filterNot { it.isSystemApp }
        if (sideloaded.isNotEmpty()) {
            add(
                WatcherFinding(
                    WatcherLevel.CHECK,
                    "Apps installed from outside an app store",
                    "These were installed from a file or another app rather than a store. Garuda Sentinel itself " +
                        "is one of them if you installed it from a file.",
                    sideloaded,
                    WatcherKind.SIDELOADED,
                )
            )
        }
        if (signals.usbDebuggingEnabled || signals.developerOptionsEnabled) {
            val what = when {
                signals.usbDebuggingEnabled -> "USB debugging is on, so a computer with a cable can read and control much of the phone."
                else -> "Developer options are on. On its own this changes little, but it allows USB debugging."
            }
            add(WatcherFinding(WatcherLevel.CHECK, "Developer settings", what, kind = WatcherKind.DEVELOPER))
        }
        signals.defaultSmsApp?.let {
            add(
                WatcherFinding(
                    WatcherLevel.FINE,
                    "App that handles your text messages",
                    "The default messaging app can read and send SMS. It should be one you chose.",
                    listOf(it),
                    WatcherKind.DEFAULT_SMS,
                )
            )
        }
        if (signals.screenLockEnabled) {
            add(WatcherFinding(WatcherLevel.FINE, "Screen lock is on", "The phone asks for your PIN, pattern, password or biometrics.", kind = WatcherKind.SCREEN_LOCK_ON))
        }
        if (signals.unavailable.isNotEmpty()) {
            add(
                WatcherFinding(
                    WatcherLevel.FINE,
                    "Not checked on this phone",
                    signals.unavailable.joinToString(" "),
                    kind = WatcherKind.NOT_CHECKED,
                )
            )
        }
    }

    /** Findings grouped for display, most serious first. */
    fun grouped(signals: WatcherSignals): Map<WatcherLevel, List<WatcherFinding>> =
        findings(signals).groupBy { it.level }
            .toSortedMap(compareBy { it.ordinal })

    fun attentionCount(signals: WatcherSignals): Int = findings(signals).count { it.level == WatcherLevel.ATTENTION }
}
