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

package com.corbraytechnologies.garudasentinel.findings

import com.corbraytechnologies.garudasentinel.model.SnapshotApp
import com.corbraytechnologies.garudasentinel.model.WatcherApp
import com.corbraytechnologies.garudasentinel.model.WatcherFinding
import com.corbraytechnologies.garudasentinel.model.WatcherKind
import com.corbraytechnologies.garudasentinel.model.WatcherLevel
import com.corbraytechnologies.garudasentinel.utils.PermissionChange
import com.corbraytechnologies.garudasentinel.utils.ScanChanges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class ReportTextTest {

    private fun finding(severity: FindingSeverity) = Finding(severity, "t", "s", FindingAction.SeePlaces)

    @Test
    fun `headline before the first scan, with nothing found, and with findings`() {
        assertEquals("Run your first check", ReportText.headline(null, hasScanned = false))
        assertEquals("Nothing needs a look", ReportText.headline(Report(emptyList(), 3, emptyList()), hasScanned = true))
        assertEquals("1 thing worth a look", ReportText.headline(Report(listOf(finding(FindingSeverity.LOW)), 0, emptyList()), true))
        assertEquals(
            "4 things worth a look",
            ReportText.headline(Report(List(4) { finding(FindingSeverity.MEDIUM) }, 0, emptyList()), true),
        )
    }

    @Test
    fun `not checked rows do not count toward the headline`() {
        val report = Report(emptyList(), 0, listOf(NotChecked("Photos were not checked", "s", FindingAction.SetUp)))
        assertEquals("Nothing needs a look", ReportText.headline(report, hasScanned = true))
    }

    @Test
    fun `tally leaves out empty groups but always shows passed`() {
        val report = Report(
            listOf(finding(FindingSeverity.HIGH), finding(FindingSeverity.LOW), finding(FindingSeverity.LOW)),
            passed = 0,
            notChecked = emptyList(),
        )
        assertEquals(
            listOf(TallyPart("1 high", FindingSeverity.HIGH), TallyPart("2 low", FindingSeverity.LOW), TallyPart("0 passed", null)),
            ReportText.tally(report),
        )
    }

    @Test
    fun `since label is the previous scan date in capitals`() {
        // 2026-10-03 12:00 UTC
        assertEquals("SINCE OCT 3", ReportText.sinceLabel(1_791_028_800_000L, Locale.US, TimeZone.getTimeZone("UTC")))
    }

    @Test
    fun `change lines put added items first, removed items last, and stop at five`() {
        val app = { name: String -> SnapshotApp(name, name) }
        val changes = ScanChanges(
            previousAt = 0,
            newApps = listOf(app("New1"), app("New2"), app("New3")),
            removedApps = listOf(app("Gone")),
            newPermissions = listOf(PermissionChange(app("Cam"), listOf("Camera"))),
            newWatcherSignals = listOf("USB debugging was turned on"),
        )
        val lines = ReportText.changeLines(changes)
        assertEquals(ReportText.MAX_CHANGE_LINES, lines.size)
        assertEquals(ChangeLine(true, "USB debugging was turned on"), lines[0])
        assertEquals(ChangeLine(true, "Cam was allowed camera"), lines[1])
        assertEquals(ChangeLine(true, "New1 was installed"), lines[2])
        assertTrue(lines.all { it.added })

        val short = ReportText.changeLines(ScanChanges(previousAt = 0, removedApps = listOf(app("Gone"))))
        assertEquals(listOf(ChangeLine(false, "Gone was removed")), short)
    }

    @Test
    fun `wording joins names and switches to a count above three`() {
        assertEquals("A", Wording.list(listOf("A")))
        assertEquals("A and B", Wording.list(listOf("A", "B")))
        assertEquals("A, B and C", Wording.list(listOf("A", "B", "C")))
        assertEquals("4 apps", Wording.subject(listOf("A", "B", "C", "D"), "app"))
        assertEquals("A, B, C and 2 more", WatcherRows.names(listOf("A", "B", "C", "D", "E")))
    }

    @Test
    fun `watcher rows have a mono count line and compact all clear rows`() {
        val app = WatcherApp("com.example.watch", "Watch", isSystemApp = false)
        val attention = WatcherFinding(WatcherLevel.ATTENTION, "Apps that can read your notifications", "e", listOf(app), WatcherKind.NOTIFICATIONS)
        assertEquals("1 APP · WATCH", WatcherRows.countLine(attention, Locale.US))
        val cert = attention.copy(kind = WatcherKind.CERTIFICATES, apps = listOf(app, app))
        assertEquals("2 CERTIFICATES · WATCH AND WATCH", WatcherRows.countLine(cert, Locale.US))
        assertNull(WatcherRows.countLine(attention.copy(apps = emptyList(), kind = WatcherKind.NO_SCREEN_LOCK)))

        assertEquals("1 app: Watch", WatcherRows.checkLine(attention.copy(level = WatcherLevel.CHECK)))
        assertEquals("e", WatcherRows.checkLine(attention.copy(apps = emptyList())))

        val lock = WatcherFinding(WatcherLevel.FINE, "Screen lock is on", "e", kind = WatcherKind.SCREEN_LOCK_ON)
        assertEquals(AllClearRow("Screen lock", "On", isOn = true), WatcherRows.allClear(lock))
        val sms = WatcherFinding(WatcherLevel.FINE, "App that handles your text messages", "e", listOf(app), WatcherKind.DEFAULT_SMS)
        assertEquals(AllClearRow("Text messages app", "Watch"), WatcherRows.allClear(sms))
    }
}
