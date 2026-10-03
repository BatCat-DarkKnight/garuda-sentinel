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

/** Where an app came from, judged from its installer. Pure, so the rule is unit tested. */
object InstallSource {

    /** Known app stores, by installer package, with the name shown to the user. */
    val STORES: Map<String, String> = mapOf(
        "com.android.vending" to "Google Play Store",
        "com.sec.android.app.samsungapps" to "Galaxy Store",
        "com.amazon.venezia" to "Amazon Appstore",
        "com.huawei.appmarket" to "Huawei AppGallery",
    )

    /** The system package installers, which install an APK file the user opened. */
    private val FILE_INSTALLERS = setOf("com.google.android.packageinstaller", "com.android.packageinstaller")

    /**
     * True when the app came from a known store: either a store installed it, or a companion
     * app installed it that itself came from a store and is signed with the same certificate
     * (for example an earbuds plugin installed by its manager app). Anything else, including an
     * app installed by a store app from a different developer, counts as outside a store.
     *
     * [installerOfInstaller] is the installer of [installer]; [sameSigner] says whether the app
     * and [installer] share a signing certificate.
     */
    fun isFromStore(installer: String?, installerOfInstaller: String?, sameSigner: Boolean): Boolean =
        installer in STORES || (installer != null && installerOfInstaller in STORES && sameSigner)

    /** "Installed by Google Play Store", "Installed from a file", "Installed by <app name>". */
    fun label(installer: String?, nameOf: (String) -> String?): String = when {
        installer == null -> "Installer unknown, or came with the phone"
        installer in FILE_INSTALLERS -> "Installed from a file"
        else -> "Installed by " + (STORES[installer] ?: nameOf(installer) ?: installer)
    }
}
