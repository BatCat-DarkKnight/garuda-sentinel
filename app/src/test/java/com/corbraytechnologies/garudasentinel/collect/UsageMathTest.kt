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

package com.corbraytechnologies.garudasentinel.collect

import com.corbraytechnologies.garudasentinel.collect.UsageMath.ACTIVITY_RESUMED
import com.corbraytechnologies.garudasentinel.collect.UsageMath.DEVICE_SHUTDOWN
import com.corbraytechnologies.garudasentinel.collect.UsageMath.DEVICE_STARTUP
import com.corbraytechnologies.garudasentinel.collect.UsageMath.Event
import com.corbraytechnologies.garudasentinel.collect.UsageMath.SCREEN_INTERACTIVE
import com.corbraytechnologies.garudasentinel.collect.UsageMath.SCREEN_NON_INTERACTIVE
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageMathTest {

    @Test
    fun `consecutive resumes in the same app count as one open`() {
        val events = listOf(
            Event("mail", ACTIVITY_RESUMED, 1),
            Event("mail", ACTIVITY_RESUMED, 2),
            Event("maps", ACTIVITY_RESUMED, 3),
            Event("mail", ACTIVITY_RESUMED, 4),
        )
        assertEquals(mapOf("mail" to 2, "maps" to 1), UsageMath.countOpens(events))
    }

    @Test
    fun `opens before the cutoff are ignored but still set the foreground app`() {
        val events = listOf(
            Event("mail", ACTIVITY_RESUMED, 5),
            Event("mail", ACTIVITY_RESUMED, 15),
            Event("maps", ACTIVITY_RESUMED, 20),
        )
        assertEquals(mapOf("maps" to 1), UsageMath.countOpens(events, since = 10))
    }

    @Test
    fun `screen events that are not resumes do not count as opens`() {
        val events = listOf(
            Event("", SCREEN_INTERACTIVE, 1),
            Event("mail", ACTIVITY_RESUMED, 2),
        )
        assertEquals(mapOf("mail" to 1), UsageMath.countOpens(events))
    }

    @Test
    fun `last used is the latest resume per app`() {
        val events = listOf(
            Event("mail", ACTIVITY_RESUMED, 10),
            Event("maps", ACTIVITY_RESUMED, 20),
            Event("mail", ACTIVITY_RESUMED, 30),
        )
        assertEquals(mapOf("mail" to 30L, "maps" to 20L), UsageMath.lastUsed(events))
    }

    @Test
    fun `screen on time sums on-off intervals`() {
        val events = listOf(
            Event("", SCREEN_INTERACTIVE, 100),
            Event("", SCREEN_NON_INTERACTIVE, 150),
            Event("", SCREEN_INTERACTIVE, 200),
            Event("", SCREEN_NON_INTERACTIVE, 260),
        )
        assertEquals(110L, UsageMath.screenOnMillis(events, start = 0, end = 1000))
    }

    @Test
    fun `screen already on at start and still on at end is counted from start to end`() {
        val events = listOf(
            Event("", SCREEN_NON_INTERACTIVE, 100),
            Event("", SCREEN_INTERACTIVE, 400),
        )
        // On from 0 to 100, then from 400 to 500.
        assertEquals(200L, UsageMath.screenOnMillis(events, start = 0, end = 500))
    }

    @Test
    fun `no screen events means zero`() {
        assertEquals(0L, UsageMath.screenOnMillis(emptyList(), start = 0, end = 500))
    }

    @Test
    fun `screen events outside the window are ignored`() {
        val events = listOf(
            Event("", SCREEN_INTERACTIVE, 50),
            Event("", SCREEN_NON_INTERACTIVE, 80),
            Event("", SCREEN_INTERACTIVE, 150),
            Event("", SCREEN_NON_INTERACTIVE, 170),
        )
        assertEquals(20L, UsageMath.screenOnMillis(events, start = 100, end = 1000))
    }

    @Test
    fun `going back to an app through the launcher counts as a new open`() {
        val events = listOf(
            Event("app.a", ACTIVITY_RESUMED, 1),
            Event("launcher", ACTIVITY_RESUMED, 2),
            Event("app.a", ACTIVITY_RESUMED, 3),
        )
        assertEquals(2, UsageMath.countOpens(events)["app.a"])
    }

    @Test
    fun `a restart ends the on interval and the screen counts as off until it turns on again`() {
        // Same shape as the Samsung log: on, shutdown with no "screen off", startup, on, off.
        val events = listOf(
            Event("", SCREEN_INTERACTIVE, 100),
            Event("", DEVICE_SHUTDOWN, 600),
            Event("", DEVICE_STARTUP, 700),
            Event("", SCREEN_INTERACTIVE, 700),
            Event("", SCREEN_NON_INTERACTIVE, 750),
        )
        assertEquals(550L, UsageMath.screenOnMillis(events, start = 0, end = 1000))
    }

    @Test
    fun `a device that is still off at the end is not counted as on`() {
        // Emulator case: on, shutdown, and nothing else until the end of the range.
        val events = listOf(
            Event("", SCREEN_INTERACTIVE, 100),
            Event("", DEVICE_SHUTDOWN, 100),
        )
        assertEquals(0L, UsageMath.screenOnMillis(events, start = 0, end = 50_000))
    }

    @Test
    fun `a shutdown as the first event means the screen was on from the start`() {
        val events = listOf(
            Event("", DEVICE_SHUTDOWN, 300),
            Event("", DEVICE_STARTUP, 900),
        )
        assertEquals(300L, UsageMath.screenOnMillis(events, start = 0, end = 1000))
    }

    @Test
    fun `screen on logged just before the startup record still counts`() {
        // Emulator log: SCREEN_INTERACTIVE arrives a moment before DEVICE_STARTUP during boot.
        val events = listOf(
            Event("", SCREEN_INTERACTIVE, 100),
            Event("", DEVICE_SHUTDOWN, 100),
            Event("", SCREEN_INTERACTIVE, 100_000),
            Event("", DEVICE_STARTUP, 100_250),
        )
        assertEquals(20_000L, UsageMath.screenOnMillis(events, start = 0, end = 120_000))
    }

    @Test
    fun `an interval left open by a crash is dropped at the next startup`() {
        // No shutdown record: the phone died with the screen on. Its real end is unknown.
        val events = listOf(
            Event("", SCREEN_INTERACTIVE, 1_000),
            Event("", DEVICE_STARTUP, 500_000),
            Event("", SCREEN_INTERACTIVE, 500_100),
            Event("", SCREEN_NON_INTERACTIVE, 500_600),
        )
        assertEquals(500L, UsageMath.screenOnMillis(events, start = 0, end = 900_000))
    }
}
