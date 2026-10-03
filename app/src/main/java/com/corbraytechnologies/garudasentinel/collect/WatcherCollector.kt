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

package com.corbraytechnologies.garudasentinel.collect

import android.Manifest
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import android.view.accessibility.AccessibilityManager
import com.corbraytechnologies.garudasentinel.data.AppMetadataEntity
import com.corbraytechnologies.garudasentinel.model.WatcherApp
import com.corbraytechnologies.garudasentinel.model.WatcherSignals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.security.cert.X509Certificate

/**
 * Reads who else can watch this phone, using public APIs only and changing nothing.
 * App-derived signals come from the last scan, so no second pass over every package is needed.
 */
class WatcherCollector(private val context: Context) {

    suspend fun collect(apps: List<AppMetadataEntity>): WatcherSignals = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val byPackage = apps.associateBy { it.packageName }
        val unavailable = mutableListOf<String>()

        fun ref(packageName: String, detail: String? = null): WatcherApp {
            val known = byPackage[packageName]
            val label = known?.appName ?: runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            }.getOrNull() ?: packageName
            val system = known?.isSystemApp ?: runCatching {
                (pm.getApplicationInfo(packageName, 0).flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            }.getOrDefault(false)
            return WatcherApp(packageName, label, system, detail)
        }

        val accessibility = runCatching {
            val manager = context.getSystemService(AccessibilityManager::class.java)
            manager.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .map { ref(it.resolveInfo.serviceInfo.packageName) }
                .distinctBy { it.packageName }
        }.getOrElse {
            unavailable += "Accessibility services could not be read."
            emptyList()
        }

        // ENABLED_NOTIFICATION_LISTENERS is not a public constant, but the setting itself is readable.
        val listeners = runCatching {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                .orEmpty()
                .split(':')
                .filter { it.isNotBlank() }
                .mapNotNull { ComponentName.unflattenFromString(it)?.packageName }
                .distinct()
                .map { ref(it) }
        }.getOrElse {
            unavailable += "Notification access could not be read."
            emptyList()
        }

        val admins = runCatching {
            context.getSystemService(DevicePolicyManager::class.java)
                ?.activeAdmins.orEmpty()
                .map { ref(it.packageName) }
                .distinctBy { it.packageName }
        }.getOrElse {
            unavailable += "Device administrators could not be read."
            emptyList()
        }

        val background = apps.filter {
            pm.checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, it.packageName) == PackageManager.PERMISSION_GRANTED
        }.map { ref(it.packageName) }

        val launcherPackages = runCatching {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val flags = PackageManager.MATCH_ALL.toLong()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }.map { it.activityInfo.packageName }.toSet()
        }.getOrElse {
            unavailable += "Launcher icons could not be listed."
            emptySet()
        }
        val hidden = apps.filter { !it.isSystemApp && it.packageName !in launcherPackages }.map { ref(it.packageName) }

        val sideloaded = apps.filter { !it.isSystemApp && isOutsideAStore(it.installer) }
            .map { ref(it.packageName, detail = installerDetail(it.installer)) }

        val certificates = runCatching {
            val store = KeyStore.getInstance("AndroidCAStore").apply { load(null) }
            store.aliases().toList()
                .filter { it.startsWith("user:") }
                .map { alias ->
                    val subject = (store.getCertificate(alias) as? X509Certificate)?.subjectX500Principal?.name
                    WatcherApp(packageName = alias, appName = commonName(subject) ?: alias, isSystemApp = false, detail = "Added by hand")
                }
        }.getOrElse {
            unavailable += "Certificates could not be read."
            emptyList()
        }

        val sms = runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()?.let { ref(it) }

        val keyguard = context.getSystemService(KeyguardManager::class.java)
        val resolver = context.contentResolver

        WatcherSignals(
            accessibilityServices = accessibility,
            notificationListeners = listeners,
            deviceAdmins = admins,
            backgroundLocationApps = background,
            installedFromOutsideAStore = sideloaded,
            appsWithoutLauncherIcon = hidden,
            userCaCertificates = certificates,
            defaultSmsApp = sms,
            screenLockEnabled = keyguard?.isDeviceSecure ?: true,
            developerOptionsEnabled = Settings.Global.getInt(resolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1,
            usbDebuggingEnabled = Settings.Global.getInt(resolver, Settings.Global.ADB_ENABLED, 0) == 1,
            unavailable = unavailable,
        )
    }

    private fun isOutsideAStore(installer: String?): Boolean = when (installer) {
        null -> true
        "com.android.vending", "com.sec.android.app.samsungapps", "com.amazon.venezia", "com.huawei.appmarket" -> false
        else -> true
    }

    private fun installerDetail(installer: String?): String = when (installer) {
        null -> "Installed from a file, or came with the phone"
        "com.google.android.packageinstaller", "com.android.packageinstaller" -> "Installed from a file"
        else -> "Installed by $installer"
    }

    private fun commonName(subject: String?): String? =
        subject?.split(',')?.firstOrNull { it.trim().startsWith("CN=") }?.trim()?.removePrefix("CN=")
}
