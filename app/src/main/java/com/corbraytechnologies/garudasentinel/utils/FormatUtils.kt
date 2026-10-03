package com.corbraytechnologies.garudasentinel.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    if (size < 1024) return "$size B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
    return String.format(Locale.getDefault(), "%.1f %s", size / 1024.0.pow(digitGroups), units[digitGroups])
}

fun formatDate(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

/** "2h 05m", "12m", "40s", or "0s". */
fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> String.format(Locale.getDefault(), "%dh %02dm", hours, minutes)
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

/** Joins the non-blank parts of a detail line with a separator, so empty fields leave no stray "|". */
fun detailLine(vararg parts: String?): String =
    parts.filterNot { it.isNullOrBlank() }.joinToString("  |  ")

/** "1 app", "3 apps". [plural] defaults to [singular] plus "s". */
fun countOf(count: Int, singular: String, plural: String = singular + "s"): String =
    "$count ${if (count == 1) singular else plural}"
