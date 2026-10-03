package com.corbraytechnologies.garudasentinel.utils

import com.corbraytechnologies.garudasentinel.data.AppMetadataEntity

/** Shared app list rules, so every screen counts "apps you installed" the same way. */
object AppLists {

    /** Apps the user installed. Garuda Sentinel itself is left out; it still shows under "All". */
    fun userInstalled(apps: List<AppMetadataEntity>, ownPackage: String): List<AppMetadataEntity> =
        apps.filter { !it.isSystemApp && it.packageName != ownPackage }
}
