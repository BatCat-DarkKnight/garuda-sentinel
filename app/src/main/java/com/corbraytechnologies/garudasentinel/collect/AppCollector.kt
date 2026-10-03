package com.corbraytechnologies.garudasentinel.collect

import androidx.core.net.toUri
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.corbraytechnologies.garudasentinel.data.AppMetadataEntity
import com.corbraytechnologies.garudasentinel.utils.AppCategorizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppCollector(private val context: Context) {

    suspend fun collect(): List<AppMetadataEntity> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val browsers = browserPackages(pm)
        installedPackages(pm).mapNotNull { info ->
            val appInfo = info.applicationInfo ?: return@mapNotNull null
            val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            AppMetadataEntity(
                packageName = info.packageName,
                appName = pm.getApplicationLabel(appInfo).toString(),
                versionName = info.versionName.orEmpty(),
                versionCode = info.longVersionCode,
                firstInstallTime = info.firstInstallTime,
                lastUpdateTime = info.lastUpdateTime,
                targetSdkVersion = appInfo.targetSdkVersion,
                minSdkVersion = appInfo.minSdkVersion,
                permissions = info.requestedPermissions?.joinToString(",").orEmpty(),
                grantedPermissions = grantedOf(info).joinToString(","),
                isSystemApp = isSystem,
                appCategory = AppCategorizer.categorizeApp(
                    packageName = info.packageName,
                    androidCategory = appInfo.category,
                    isBrowser = info.packageName in browsers,
                    isSystemApp = isSystem,
                ),
                installer = installerOf(pm, info.packageName),
            )
        }
    }

    private fun grantedOf(info: PackageInfo): List<String> {
        val names = info.requestedPermissions ?: return emptyList()
        val flags = info.requestedPermissionsFlags ?: return emptyList()
        return names.filterIndexed { i, _ ->
            i < flags.size && flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
        }
    }

    private fun installedPackages(pm: PackageManager): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }

    private fun installerOf(pm: PackageManager, packageName: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(packageName)
        }
    } catch (_: Exception) {
        null
    }

    companion object {
        /**
         * Package of the default home screen (launcher), found by resolving the HOME intent.
         * Null when no default is set (Android then resolves to its own chooser).
         */
        fun defaultLauncherPackage(pm: PackageManager): String? {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            return info?.activityInfo?.packageName?.takeIf { it != "android" }
        }

        /**
         * Packages that can open web links. Only resolves an intent locally;
         * no network request is made.
         */
        fun browserPackages(pm: PackageManager): Set<String> {
            val intent = Intent(Intent.ACTION_VIEW, "https://example.com".toUri())
                .addCategory(Intent.CATEGORY_BROWSABLE)
            val matches = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
            return matches.map { it.activityInfo.packageName }.toSet()
        }
    }
}
