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

package com.corbraytechnologies.garudasentinel.utils

/** Plain-language names for permissions that reveal personal data or behavior. */
object SensitivePermissions {

    data class Info(val label: String, val why: String)

    val all: Map<String, Info> = mapOf(
        "android.permission.ACCESS_FINE_LOCATION" to Info("Precise location", "Where you are, to within a few meters."),
        "android.permission.ACCESS_COARSE_LOCATION" to Info("Approximate location", "Which neighborhood or city you are in."),
        "android.permission.ACCESS_BACKGROUND_LOCATION" to Info("Location in background", "Where you go, even when the app is closed."),
        "android.permission.CAMERA" to Info("Camera", "Can take photos and video."),
        "android.permission.RECORD_AUDIO" to Info("Microphone", "Can record sound."),
        "android.permission.READ_CONTACTS" to Info("Contacts", "Names and numbers of people you know."),
        "android.permission.READ_CALL_LOG" to Info("Call log", "Who you called, and when."),
        "android.permission.READ_SMS" to Info("Text messages", "The content of your SMS messages."),
        "android.permission.READ_CALENDAR" to Info("Calendar", "Your appointments and meetings."),
        "android.permission.READ_PHONE_STATE" to Info("Phone status", "Your number, carrier and call state."),
        "android.permission.BODY_SENSORS" to Info("Body sensors", "Heart rate and other health sensors."),
        "android.permission.ACTIVITY_RECOGNITION" to Info("Physical activity", "Whether you walk, run, cycle or drive."),
        "android.permission.READ_MEDIA_IMAGES" to Info("Photos", "Your photo library."),
        "android.permission.READ_MEDIA_VIDEO" to Info("Videos", "Your video library."),
        "android.permission.READ_EXTERNAL_STORAGE" to Info("Shared storage", "Photos, videos and files in shared storage."),
        "android.permission.MANAGE_EXTERNAL_STORAGE" to Info("All files", "Every file in shared storage."),
        "android.permission.ACCESS_MEDIA_LOCATION" to Info("Photo locations", "Where your photos were taken."),
        "android.permission.QUERY_ALL_PACKAGES" to Info("All installed apps", "The full list of apps on your phone."),
        "android.permission.PACKAGE_USAGE_STATS" to Info("App usage", "Which apps you use, and for how long."),
        "android.permission.SYSTEM_ALERT_WINDOW" to Info("Draw over other apps", "Can show content on top of other apps."),
        "android.permission.BLUETOOTH_SCAN" to Info("Nearby Bluetooth devices", "Devices around you, which can reveal location."),
        "android.permission.NEARBY_WIFI_DEVICES" to Info("Nearby Wi-Fi devices", "Networks and devices around you."),
        "android.permission.GET_ACCOUNTS" to Info("Accounts", "Which accounts are signed in on the phone."),
    )

    fun sensitiveOf(permissions: List<String>): List<Info> = permissions.mapNotNull { all[it] }.distinct()

    /**
     * Special access that users turn on in Settings. Android tracks these as app-ops, so the
     * package manager always reports them as not granted, and another app's state cannot be read.
     */
    val specialAccess: Set<String> = setOf(
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
    )

    data class Breakdown(val allowed: List<Info>, val notAllowed: List<Info>, val unknown: List<Info>)

    /**
     * Splits an app's sensitive permissions into allowed, not allowed and unknown.
     * [knownSpecial] holds special-access states that could be checked (only possible for this
     * app itself); other special access is reported as unknown rather than "not allowed".
     */
    fun breakdown(requested: List<String>, granted: List<String>, knownSpecial: Map<String, Boolean> = emptyMap()): Breakdown {
        val sensitive = requested.filter { it in all }.distinct()
        val allowed = mutableListOf<Info>()
        val notAllowed = mutableListOf<Info>()
        val unknown = mutableListOf<Info>()
        for (permission in sensitive) {
            val info = all.getValue(permission)
            when {
                permission in specialAccess -> when (knownSpecial[permission]) {
                    true -> allowed += info
                    false -> notAllowed += info
                    null -> unknown += info
                }
                permission in granted -> allowed += info
                else -> notAllowed += info
            }
        }
        return Breakdown(allowed.distinct(), notAllowed.distinct(), unknown.distinct())
    }
}
