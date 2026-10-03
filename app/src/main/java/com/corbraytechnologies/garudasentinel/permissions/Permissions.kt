package com.corbraytechnologies.garudasentinel.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat

/** How much of the photo and video library the app can read. */
enum class MediaAccess { FULL, PARTIAL, NONE }

/**
 * Every permission in this app is optional. Nothing here blocks navigation;
 * screens call these checks to explain what is missing and why.
 */
object Permissions {

    /** Runtime permissions needed to read photos, videos and audio on this Android version. */
    fun mediaPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /** Request COARSE together with FINE; Android 12+ ignores a FINE-only request. */
    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    const val MEDIA_LOCATION = Manifest.permission.ACCESS_MEDIA_LOCATION

    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun mediaAccess(context: Context): MediaAccess = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> when {
            isGranted(context, Manifest.permission.READ_MEDIA_IMAGES) ||
                isGranted(context, Manifest.permission.READ_MEDIA_VIDEO) -> MediaAccess.FULL
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                isGranted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> MediaAccess.PARTIAL
            else -> MediaAccess.NONE
        }
        isGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE) -> MediaAccess.FULL
        else -> MediaAccess.NONE
    }

    fun hasAudioAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(context, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            isGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun hasMediaLocation(context: Context): Boolean = isGranted(context, MEDIA_LOCATION)

    fun hasAnyLocation(context: Context): Boolean =
        locationPermissions.any { isGranted(context, it) }

    fun hasPreciseLocation(context: Context): Boolean =
        isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        // Deprecated on newer SDKs, but it is the only op check available on minSdk 29.
        @Suppress("DEPRECATION")
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            isGranted(context, Manifest.permission.PACKAGE_USAGE_STATS)
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    /** Opens the system Usage access screen, pointed at this app where the OS supports it. */
    fun usageAccessSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }

    fun appDetailsSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
}
