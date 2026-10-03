package com.corbraytechnologies.garudasentinel.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import com.corbraytechnologies.garudasentinel.findings.SettingsTarget

/**
 * The standard Settings intent for each screen a finding can open. These only open a screen;
 * any change is made by the user there. [openSettings] falls back to the main Settings screen
 * when a phone does not have one of these.
 */
fun SettingsTarget.intent(): Intent = Intent(
    when (this) {
        SettingsTarget.ACCESSIBILITY -> Settings.ACTION_ACCESSIBILITY_SETTINGS
        SettingsTarget.NOTIFICATION_ACCESS -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        SettingsTarget.SECURITY -> Settings.ACTION_SECURITY_SETTINGS
        SettingsTarget.LOCATION -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
        SettingsTarget.DEVELOPER_OPTIONS -> Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
        SettingsTarget.ALL_APPS -> Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
        SettingsTarget.DEFAULT_APPS -> Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS
    },
)

/** The system App info screen for one app. */
fun appInfoIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
