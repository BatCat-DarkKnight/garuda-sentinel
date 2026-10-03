package com.corbraytechnologies.garudasentinel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corbraytechnologies.garudasentinel.collect.AppCollector
import com.corbraytechnologies.garudasentinel.collect.MediaCollector
import com.corbraytechnologies.garudasentinel.permissions.Permissions
import com.corbraytechnologies.garudasentinel.ui.Dest
import com.corbraytechnologies.garudasentinel.ui.MainViewModel
import com.corbraytechnologies.garudasentinel.ui.components.GarudaScaffold
import com.corbraytechnologies.garudasentinel.ui.components.KeyValueRow
import com.corbraytechnologies.garudasentinel.ui.components.SectionCard
import com.corbraytechnologies.garudasentinel.utils.AppCategorizer
import com.corbraytechnologies.garudasentinel.utils.AppLists
import com.corbraytechnologies.garudasentinel.utils.MediaDates
import com.corbraytechnologies.garudasentinel.utils.countOf
import com.corbraytechnologies.garudasentinel.utils.formatDate
import com.corbraytechnologies.garudasentinel.utils.formatDuration
import kotlin.math.roundToInt

/**
 * Plain-language explanation of what the collected data could reveal.
 * Every number here is computed from the user's own scan; nothing is estimated or invented.
 */
@Composable
fun YourDataScreen(main: MainViewModel, onMenuClick: () -> Unit, onNavigate: (Dest) -> Unit) {
    val apps by main.apps.collectAsStateWithLifecycle()
    val media by main.media.collectAsStateWithLifecycle()
    val files by main.files.collectAsStateWithLifecycle()
    val usage by main.usage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (usage == null && Permissions.hasUsageAccess(context)) main.refreshUsage()
    }

    // Apps the user installed (same rule as the Apps screen). The home screen app is left out of
    // the interests below, because every phone has one and it says nothing about the user.
    val launcher = remember { AppCollector.defaultLauncherPackage(context.packageManager) }
    val userApps = remember(apps) { AppLists.userInstalled(apps, context.packageName) }
    val photos = remember(media) { media.filter { it.type == MediaCollector.TYPE_IMAGE } }
    val gpsPhotos = remember(photos) { photos.filter { it.gpsLat != null && it.gpsLong != null } }
    // Round to about 1 km so nearby shots count as one place.
    val places = remember(gpsPhotos) {
        gpsPhotos.map { (it.gpsLat!! * 100).roundToInt() to (it.gpsLong!! * 100).roundToInt() }.distinct().size
    }
    val categories = remember(userApps, launcher) {
        userApps.filter { it.packageName != launcher }.map { it.appCategory }
            .filter { it != AppCategorizer.OTHER && it != AppCategorizer.SYSTEM }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(5)
    }

    GarudaScaffold(title = "Your Data", onMenuClick = onMenuClick) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Metadata is information about your data: when a photo was taken and where, which apps you use and when, " +
                        "what your phone is. One piece says little. Put together, it describes your life. Here is what this phone holds.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (apps.isEmpty() && media.isEmpty()) {
                item {
                    OutlinedButton(onClick = { onNavigate(Dest.HOME) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Nothing scanned yet. Go to Home and tap START SCAN.")
                    }
                }
                return@LazyColumn
            }

            item {
                SectionCard(
                    title = "Where you have been",
                    subtitle = "Most phone cameras save GPS coordinates inside every photo. Anyone you send the original file to can read them.",
                ) {
                    KeyValueRow("Photos with location", "${gpsPhotos.size} of ${photos.size}")
                    KeyValueRow("Distinct places (about 1 km)", if (gpsPhotos.isEmpty()) null else places.toString())
                    KeyValueRow(
                        "Oldest located photo",
                        MediaDates.oldest(gpsPhotos, { it.dateTaken }, { it.lastModified })?.let { (date, isFileDate) ->
                            formatDate(date) + if (isFileDate) " (file date)" else ""
                        },
                    )
                    if (gpsPhotos.isEmpty()) {
                        Text(
                            "No photo locations found. Either your camera does not save location, or \"photo location\" access is off.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                val u = usage
                SectionCard(
                    title = "Your daily routine",
                    subtitle = "Android records every time you open an app. Usage patterns show when you wake, work and sleep.",
                ) {
                    if (u == null) {
                        Text("Needs Usage access. Open App Usage to turn it on.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        KeyValueRow("Screen on today", formatDuration(u.screenOnTodayMs))
                        KeyValueRow("App opens today", u.apps.sumOf { it.opensToday }.toString())
                        KeyValueRow("Most used this week", u.apps.firstOrNull()?.appName)
                        KeyValueRow("Browsing time this week", formatDuration(u.apps.filter { it.isBrowser }.sumOf { it.foregroundLast7DaysMs }))
                    }
                }
            }

            item {
                SectionCard(
                    title = "Your interests",
                    subtitle = "The apps you install hint at your health, money, beliefs and relationships. Categories here are estimates from app names.",
                ) {
                    KeyValueRow("Apps you installed", userApps.size.toString())
                    categories.forEach { (category, count) -> KeyValueRow(category, countOf(count, "app")) }
                }
            }

            item {
                val allowed = { perm: String -> userApps.count { perm in it.grantedPermissionList } }
                SectionCard(
                    title = "Who else can see it",
                    subtitle = "These apps you installed are allowed to read this data right now.",
                ) {
                    KeyValueRow(
                        "Location",
                        userApps.count { a ->
                            a.grantedPermissionList.any { it == "android.permission.ACCESS_FINE_LOCATION" || it == "android.permission.ACCESS_COARSE_LOCATION" }
                        }.toString(),
                    )
                    KeyValueRow("Microphone", allowed("android.permission.RECORD_AUDIO").toString())
                    KeyValueRow("Camera", allowed("android.permission.CAMERA").toString())
                    KeyValueRow("Contacts", allowed("android.permission.READ_CONTACTS").toString())
                    KeyValueRow("Photos", allowed("android.permission.READ_MEDIA_IMAGES").toString())
                    OutlinedButton(onClick = { onNavigate(Dest.APPS) }) { Text("See which apps") }
                }
            }

            item {
                val messaging = media.count { it.mediaCategory == "WhatsApp" || it.mediaCategory == "Telegram" }
                val screenshots = media.count { it.mediaCategory == "Screenshots" }
                SectionCard(
                    title = "Who you talk to",
                    subtitle = "Folder names show which messaging apps you use and how much media they hold.",
                ) {
                    KeyValueRow("Media from messaging apps", messaging.toString())
                    KeyValueRow("Screenshots", screenshots.toString())
                    KeyValueRow("Files in your chosen folder", files.size.toString())
                }
            }

            item {
                Text(
                    "Everything on this screen stays on your phone. Garuda Sentinel has no internet permission. " +
                        "You can export or delete it all from Scan History & Export.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
