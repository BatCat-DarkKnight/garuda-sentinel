package com.corbraytechnologies.garudasentinel.findings

import com.corbraytechnologies.garudasentinel.utils.ScanChanges
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One part of the tally line. [severity] is null for the "passed" part. */
data class TallyPart(val text: String, val severity: FindingSeverity?)

/** One line under "Since <date>": something added or something removed. */
data class ChangeLine(val added: Boolean, val text: String)

/** The words at the top of the Report screen. */
object ReportText {

    /** Lines shown under "Since <date>". */
    const val MAX_CHANGE_LINES = 5

    fun headline(report: Report?, hasScanned: Boolean): String {
        val n = report?.findings?.size ?: 0
        return when {
            !hasScanned -> "Run your first check"
            n == 0 -> "Nothing needs a look"
            n == 1 -> "1 thing worth a look"
            else -> "$n things worth a look"
        }
    }

    /** "1 high", "2 medium", "1 low", "7 passed". Empty severity groups are left out; "passed" always shows. */
    fun tally(report: Report): List<TallyPart> = buildList {
        FindingSeverity.entries.forEach { severity ->
            val n = report.count(severity)
            if (n > 0) add(TallyPart("$n ${severity.name.lowercase(Locale.ROOT)}", severity))
        }
        add(TallyPart("${report.passed} passed", null))
    }

    /** "SINCE OCT 3", from the previous snapshot's time. */
    fun sinceLabel(previousAt: Long, locale: Locale = Locale.getDefault(), zone: TimeZone = TimeZone.getDefault()): String {
        val format = SimpleDateFormat("MMM d", locale).apply { timeZone = zone }
        return "SINCE " + format.format(Date(previousAt)).uppercase(locale)
    }

    /**
     * Up to [MAX_CHANGE_LINES] lines from the scan comparison: new watcher signals and newly
     * granted permissions first, then installed apps, then removed apps.
     */
    fun changeLines(changes: ScanChanges): List<ChangeLine> = buildList {
        changes.newWatcherSignals.forEach { add(ChangeLine(true, it)) }
        changes.newPermissions.forEach { change ->
            add(ChangeLine(true, "${change.app.appName} was allowed " + change.permissionLabels.joinToString().lowercase()))
        }
        changes.newApps.forEach { add(ChangeLine(true, "${it.appName} was installed")) }
        changes.removedApps.forEach { add(ChangeLine(false, "${it.appName} was removed")) }
    }.take(MAX_CHANGE_LINES)
}
