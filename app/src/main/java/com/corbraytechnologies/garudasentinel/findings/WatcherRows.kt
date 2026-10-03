package com.corbraytechnologies.garudasentinel.findings

import com.corbraytechnologies.garudasentinel.model.WatcherFinding
import com.corbraytechnologies.garudasentinel.model.WatcherKind
import com.corbraytechnologies.garudasentinel.utils.countOf
import java.util.Locale

/** A compact "all clear" row: name on the left, short status on the right. [isOn] shows the status in green. */
data class AllClearRow(val name: String, val status: String, val isOn: Boolean = false)

/** Text for the rows on the Who can watch screen. */
object WatcherRows {

    /** "1 APP · SOME APP" or "4 APPS · A, B, C AND 1 MORE"; null when the finding names no app. */
    fun countLine(finding: WatcherFinding, locale: Locale = Locale.getDefault()): String? {
        if (finding.apps.isEmpty()) return null
        val noun = if (finding.kind == WatcherKind.CERTIFICATES) "certificate" else "app"
        return (countOf(finding.apps.size, noun) + " · " + names(finding.apps.map { it.appName })).uppercase(locale)
    }

    /** Second line of a "worth knowing" row: the apps involved, or the explanation when there are none. */
    fun checkLine(finding: WatcherFinding): String =
        if (finding.apps.isEmpty()) finding.explanation
        else countOf(finding.apps.size, "app") + ": " + names(finding.apps.map { it.appName })

    fun allClear(finding: WatcherFinding): AllClearRow = when (finding.kind) {
        WatcherKind.SCREEN_LOCK_ON -> AllClearRow("Screen lock", "On", isOn = true)
        WatcherKind.DEFAULT_SMS -> AllClearRow("Text messages app", finding.apps.firstOrNull()?.appName ?: "None")
        WatcherKind.SYSTEM_ACCESSIBILITY -> AllClearRow("Accessibility from your phone maker", countOf(finding.apps.size, "service"))
        WatcherKind.NOT_CHECKED -> AllClearRow("Not checked on this phone", "Not read")
        else -> AllClearRow(finding.title, "")
    }

    /** Up to three names, then "and N more". */
    fun names(names: List<String>): String =
        if (names.size <= Wording.MAX_NAMES) Wording.list(names)
        else names.take(Wording.MAX_NAMES).joinToString(", ") + " and ${names.size - Wording.MAX_NAMES} more"
}
