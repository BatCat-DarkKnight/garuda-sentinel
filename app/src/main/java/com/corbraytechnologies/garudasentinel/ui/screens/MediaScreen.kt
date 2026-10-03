package com.corbraytechnologies.garudasentinel.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.corbraytechnologies.garudasentinel.collect.MediaCollector
import com.corbraytechnologies.garudasentinel.data.MediaMetadataEntity
import com.corbraytechnologies.garudasentinel.permissions.MediaAccess
import com.corbraytechnologies.garudasentinel.permissions.Permissions
import com.corbraytechnologies.garudasentinel.ui.Dest
import com.corbraytechnologies.garudasentinel.ui.MainViewModel
import com.corbraytechnologies.garudasentinel.ui.components.EmptyStateMessage
import com.corbraytechnologies.garudasentinel.ui.components.GarudaScaffold
import com.corbraytechnologies.garudasentinel.ui.components.KeyValueRow
import com.corbraytechnologies.garudasentinel.ui.components.NoticeCard
import com.corbraytechnologies.garudasentinel.ui.components.SectionCard
import com.corbraytechnologies.garudasentinel.utils.MediaDates
import com.corbraytechnologies.garudasentinel.utils.countOf
import com.corbraytechnologies.garudasentinel.utils.detailLine
import com.corbraytechnologies.garudasentinel.utils.formatDate
import com.corbraytechnologies.garudasentinel.utils.formatDuration
import com.corbraytechnologies.garudasentinel.utils.formatFileSize
import java.util.Locale
import com.corbraytechnologies.garudasentinel.ui.theme.Palette

private enum class MediaFilter(val label: String, val type: String?) {
    ALL("All", null), PHOTOS("Photos", MediaCollector.TYPE_IMAGE), VIDEOS("Videos", MediaCollector.TYPE_VIDEO),
    AUDIO("Audio", MediaCollector.TYPE_AUDIO), WITH_GPS("With location", null),
}

/** Large libraries are summarized in full but only the newest items are listed. */
private const val LIST_LIMIT = 500

@Composable
fun MediaScreen(main: MainViewModel, onBack: () -> Unit, onNavigate: (Dest) -> Unit, locatedOnly: Boolean = false) {
    val context = LocalContext.current
    val media by main.media.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(if (locatedOnly) MediaFilter.WITH_GPS else MediaFilter.ALL) }
    var access by remember { mutableStateOf(MediaAccess.NONE) }
    var hasPhotoLocation by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        access = Permissions.mediaAccess(context)
        hasPhotoLocation = Permissions.hasMediaLocation(context)
        onPauseOrDispose { }
    }

    val filtered = remember(media, filter) {
        when (filter) {
            MediaFilter.WITH_GPS -> media.filter { it.gpsLat != null }
            else -> filter.type?.let { t -> media.filter { it.type == t } } ?: media
        }
    }

    GarudaScaffold(title = "Photos & Media", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (access != MediaAccess.FULL) {
                item {
                    NoticeCard(
                        title = if (access == MediaAccess.NONE) "No access to photos and videos" else "Limited access",
                        body = if (access == MediaAccess.NONE) {
                            "Grant media access to see what your photos and videos reveal."
                        } else {
                            "You allowed only some photos. Only those are scanned."
                        },
                        actionLabel = "Permissions",
                        onAction = { onNavigate(Dest.PERMISSIONS) },
                    )
                }
            } else if (!hasPhotoLocation) {
                item {
                    NoticeCard(
                        title = "Photo locations are hidden",
                        body = "Android removes GPS from photos unless you allow \"photo location\" access. Allow it to see where your photos were taken.",
                        actionLabel = "Permissions",
                        onAction = { onNavigate(Dest.PERMISSIONS) },
                    )
                }
            }
            if (media.isEmpty()) {
                item { EmptyStateMessage(Icons.Default.PermMedia, "No media yet. Run a check from Report.") }
                return@LazyColumn
            }
            item { MediaSummary(media) }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MediaFilter.entries.forEach { FilterChip(selected = filter == it, onClick = { filter = it }, label = { Text(it.label) }) }
                }
            }
            item {
                val shownCount = minOf(filtered.size, LIST_LIMIT)
                Text(
                    if (filtered.size > LIST_LIMIT) "Showing newest $shownCount of ${filtered.size}" else countOf(filtered.size, "item"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(filtered.take(LIST_LIMIT), key = { it.contentUri }) { MediaCard(it) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MediaSummary(media: List<MediaMetadataEntity>) {
    val photos = media.filter { it.type == MediaCollector.TYPE_IMAGE }
    val withGps = photos.count { it.gpsLat != null }
    val cameras = photos.mapNotNull { p -> listOfNotNull(p.cameraMake, p.cameraModel).joinToString(" ").ifBlank { null } }.distinct()
    val sources = media.groupingBy { it.mediaCategory }.eachCount().entries.sortedByDescending { it.value }.take(5)
    SectionCard(title = "What your media reveals") {
        KeyValueRow("Photos", photos.size.toString())
        KeyValueRow("Videos", media.count { it.type == MediaCollector.TYPE_VIDEO }.toString())
        KeyValueRow("Audio", media.count { it.type == MediaCollector.TYPE_AUDIO }.toString())
        KeyValueRow("Photos with GPS", "$withGps of ${photos.size}")
        KeyValueRow("Cameras seen", if (cameras.isEmpty()) "None recorded" else cameras.take(4).joinToString())
        KeyValueRow("Total size", formatFileSize(media.sumOf { it.fileSize }))
        Spacer(Modifier.height(4.dp))
        Text("Where files come from (estimated)", style = MaterialTheme.typography.titleSmall)
        sources.forEach { (source, count) -> KeyValueRow(source, count.toString()) }
    }
}

@Composable
private fun MediaCard(item: MediaMetadataEntity) {
    var expanded by rememberSaveable(item.contentUri) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Palette.Surface),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Thumbnail(item)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        detailLine(item.mediaCategory, MediaDates.label(item.dateTaken, item.lastModified), formatFileSize(item.fileSize)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.gpsLat != null && item.gpsLong != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(14.dp))
                            Text(" Location saved in this photo", style = MaterialTheme.typography.bodySmall, color = Palette.Accent)
                        }
                    }
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                KeyValueRow("Folder", item.relativePath)
                KeyValueRow("Type", item.mimeType)
                if (item.width != null && item.height != null) KeyValueRow("Size (pixels)", "${item.width} x ${item.height}")
                item.duration?.let { KeyValueRow("Length", formatDuration(it)) }
                KeyValueRow("Taken", item.dateTaken?.let { formatDate(it) } ?: "Not recorded in the file")
                KeyValueRow("File date", formatDate(item.lastModified))
                if (item.type == MediaCollector.TYPE_IMAGE) {
                    KeyValueRow("Camera", listOfNotNull(item.cameraMake, item.cameraModel).joinToString(" ").ifBlank { null })
                    KeyValueRow(
                        "GPS",
                        if (item.gpsLat != null && item.gpsLong != null) {
                            String.format(Locale.US, "%.5f, %.5f", item.gpsLat, item.gpsLong)
                        } else {
                            null
                        },
                    )
                    item.iso?.let { KeyValueRow("ISO", it.toString()) }
                    item.exposureTime?.let { KeyValueRow("Exposure", "$it s") }
                }
                item.artist?.let { KeyValueRow("Artist", it) }
                item.album?.let { KeyValueRow("Album", it) }
            }
        }
    }
}

@Composable
private fun Thumbnail(item: MediaMetadataEntity) {
    val modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
    when (item.type) {
        MediaCollector.TYPE_IMAGE, MediaCollector.TYPE_VIDEO -> AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(item.contentUri).size(168).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
        else -> Box(modifier, contentAlignment = Alignment.Center) {
            Icon(
                if (item.type == MediaCollector.TYPE_AUDIO) Icons.Default.AudioFile else Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = Palette.Accent,
            )
        }
    }
}
