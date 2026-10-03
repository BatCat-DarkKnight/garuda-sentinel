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

/**
 * Best-effort grouping of apps and media files. These are labels to help people
 * browse their data, not facts; the UI and export call them "estimated category".
 */
object AppCategorizer {

    const val BROWSER = "Browser"
    const val SYSTEM = "System"
    const val OTHER = "Other"

    /**
     * @param androidCategory the developer-declared [ApplicationInfo.category], or
     *   [ApplicationInfo.CATEGORY_UNDEFINED].
     * @param isBrowser true when the app can open web links (resolved by the caller).
     */
    fun categorizeApp(
        packageName: String,
        androidCategory: Int = ApplicationInfo.CATEGORY_UNDEFINED,
        isBrowser: Boolean = false,
        isSystemApp: Boolean = false,
    ): String {
        if (isBrowser) return BROWSER
        fromAndroidCategory(androidCategory)?.let { return it }

        val tokens = packageName.lowercase().split('.', '_', '-').toSet()
        KEYWORDS.forEach { (category, words) ->
            if (tokens.any { it in words }) return category
        }
        return if (isSystemApp) SYSTEM else OTHER
    }

    private fun fromAndroidCategory(category: Int): String? = when (category) {
        ApplicationInfo.CATEGORY_GAME -> "Games"
        ApplicationInfo.CATEGORY_AUDIO -> "Music & Audio"
        ApplicationInfo.CATEGORY_VIDEO -> "Video"
        ApplicationInfo.CATEGORY_IMAGE -> "Photos"
        ApplicationInfo.CATEGORY_SOCIAL -> "Social"
        ApplicationInfo.CATEGORY_NEWS -> "News"
        ApplicationInfo.CATEGORY_MAPS -> "Maps & Travel"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Accessibility"
        else -> null
    }

    /** Whole-token matches only, so "line" does not match "airline" or "online". */
    private val KEYWORDS: List<Pair<String, Set<String>>> = listOf(
        "Social" to setOf(
            "facebook", "instagram", "twitter", "tiktok", "musically", "snapchat", "linkedin",
            "reddit", "pinterest", "threads", "mastodon", "bluesky", "tumblr",
        ),
        "Messaging" to setOf(
            "whatsapp", "telegram", "signal", "discord", "messenger", "mms", "sms", "messaging",
            "viber", "wechat", "skype", "gm", "email", "mail", "outlook", "protonmail",
        ),
        "Finance" to setOf(
            "bank", "banking", "paypal", "venmo", "cashapp", "squareup", "coinbase", "binance",
            "robinhood", "fidelity", "chase", "wellsfargo", "capitalone", "amex", "wallet", "zelle",
        ),
        "Shopping" to setOf(
            "amazon", "ebay", "walmart", "target", "bestbuy", "costco", "etsy", "shopify",
            "aliexpress", "temu", "shein", "shopping",
        ),
        "Health & Fitness" to setOf(
            "fitness", "health", "workout", "strava", "fitbit", "myfitnesspal", "calm",
            "headspace", "meditation", "sleep", "pharmacy",
        ),
        "Maps & Travel" to setOf(
            "maps", "uber", "lyft", "airbnb", "booking", "expedia", "waze", "travel",
        ),
        "Food" to setOf("doordash", "ubereats", "grubhub", "instacart", "food"),
        "Video" to setOf("netflix", "youtube", "hulu", "disney", "hbo", "twitch", "primevideo"),
        "Music & Audio" to setOf("spotify", "pandora", "podcast", "podcasts", "audible", "music"),
    )

    fun categorizeMediaFile(relativePath: String, fileName: String): String {
        val path = relativePath.lowercase()
        val name = fileName.lowercase()
        return when {
            "whatsapp" in path -> "WhatsApp"
            "telegram" in path -> "Telegram"
            "screenshots" in path || name.startsWith("screenshot") -> "Screenshots"
            "screen recordings" in path || "screenrecord" in path -> "Screen recordings"
            // Samsung saves files received from other devices in Download/Quick Share.
            "quick share" in path || "nearby share" in path -> "Quick Share"
            // DCIM/Camera is the camera folder; other folders under DCIM are gallery albums
            // (Samsung Gallery creates albums there), so their photos may come from anywhere.
            path.startsWith("dcim/") ->
                if (path.removePrefix("dcim/").substringBefore('/').let { it.isEmpty() || it == "camera" }) "Camera" else "Gallery album"
            path.startsWith("download") -> "Downloads"
            listOf("instagram", "facebook", "snapchat", "tiktok", "twitter", "messenger")
                .any { it in path } -> "Social apps"
            path.startsWith("music/") || path.startsWith("podcasts/") -> "Music"
            path.startsWith("movies/") -> "Movies"
            path.startsWith("pictures/") -> "Pictures"
            else -> OTHER
        }
    }
}
