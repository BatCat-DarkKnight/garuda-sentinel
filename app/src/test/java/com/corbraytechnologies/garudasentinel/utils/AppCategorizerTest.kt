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

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCategorizerTest {

    @Test
    fun `browser flag wins over everything`() {
        assertEquals(
            AppCategorizer.BROWSER,
            AppCategorizer.categorizeApp("com.android.chrome", ApplicationInfo.CATEGORY_PRODUCTIVITY, isBrowser = true),
        )
    }

    @Test
    fun `developer declared category is used before keywords`() {
        assertEquals("Games", AppCategorizer.categorizeApp("com.example.bank", ApplicationInfo.CATEGORY_GAME))
    }

    @Test
    fun `keywords match whole package tokens only`() {
        assertEquals("Social", AppCategorizer.categorizeApp("com.instagram.android"))
        // "airline" and "online" contain "line" but must not be tagged Social or Messaging.
        assertEquals(AppCategorizer.OTHER, AppCategorizer.categorizeApp("com.example.airline"))
        assertEquals(AppCategorizer.OTHER, AppCategorizer.categorizeApp("com.shop.online"))
    }

    @Test
    fun `gmail is messaging`() {
        assertEquals("Messaging", AppCategorizer.categorizeApp("com.google.android.gm"))
    }

    @Test
    fun `unknown system app falls back to System`() {
        assertEquals(AppCategorizer.SYSTEM, AppCategorizer.categorizeApp("com.vendor.thing", isSystemApp = true))
    }

    @Test
    fun `media sources come from folder paths`() {
        assertEquals("Camera", AppCategorizer.categorizeMediaFile("DCIM/Camera/", "PXL_1.jpg"))
        assertEquals("Camera", AppCategorizer.categorizeMediaFile("DCIM/", "IMG_1.jpg"))
        assertEquals("Gallery album", AppCategorizer.categorizeMediaFile("DCIM/Cats/", "IMG_2.jpg"))
        assertEquals("Gallery album", AppCategorizer.categorizeMediaFile("DCIM/Trips/Seoul/", "IMG_3.jpg"))
        assertEquals("Screenshots", AppCategorizer.categorizeMediaFile("DCIM/Screenshots/", "Screenshot_2.png"))
        assertEquals("Screenshots", AppCategorizer.categorizeMediaFile("Pictures/Screenshots/", "Screenshot_1.png"))
        assertEquals("WhatsApp", AppCategorizer.categorizeMediaFile("Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/", "IMG-1.jpg"))
        assertEquals("Downloads", AppCategorizer.categorizeMediaFile("Download/", "file.jpg"))
        assertEquals("Quick Share", AppCategorizer.categorizeMediaFile("Download/Quick Share/", "IMG_1.jpg"))
        assertEquals(AppCategorizer.OTHER, AppCategorizer.categorizeMediaFile("Somewhere/", "a.jpg"))
    }
}
