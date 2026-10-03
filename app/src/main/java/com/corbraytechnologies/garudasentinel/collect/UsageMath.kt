package com.corbraytechnologies.garudasentinel.collect

/**
 * Pure calculations over usage events, kept free of Android types so they can be unit tested.
 * Event type values match android.app.usage.UsageEvents.Event.
 */
object UsageMath {
    const val ACTIVITY_RESUMED = 1
    const val SCREEN_INTERACTIVE = 15
    const val SCREEN_NON_INTERACTIVE = 16
    const val DEVICE_SHUTDOWN = 26
    const val DEVICE_STARTUP = 27

    data class Event(val packageName: String, val type: Int, val timestamp: Long)

    /**
     * Counts how many times each app was brought to the foreground. Several activity
     * resumes inside the same app in a row count as one open.
     */
    fun countOpens(events: List<Event>, since: Long = Long.MIN_VALUE): Map<String, Int> {
        val opens = mutableMapOf<String, Int>()
        var foreground: String? = null
        for (e in events) {
            if (e.type != ACTIVITY_RESUMED) continue
            if (e.packageName != foreground && e.timestamp >= since) {
                opens[e.packageName] = (opens[e.packageName] ?: 0) + 1
            }
            foreground = e.packageName
        }
        return opens
    }

    /** Latest resume per app. */
    fun lastUsed(events: List<Event>): Map<String, Long> {
        val last = mutableMapOf<String, Long>()
        for (e in events) {
            if (e.type == ACTIVITY_RESUMED && e.timestamp > (last[e.packageName] ?: Long.MIN_VALUE)) {
                last[e.packageName] = e.timestamp
            }
        }
        return last
    }

    /**
     * Total screen-on time inside [start, end] from screen on/off events.
     * If the first event seen is "screen off" or a shutdown, the screen is assumed on from [start].
     * Android logs a shutdown without a "screen off" first, so a shutdown ends the on interval,
     * and a startup means the screen was off until the next "screen on".
     */
    fun screenOnMillis(events: List<Event>, start: Long, end: Long): Long {
        val screenEvents = events.filter { it.type in SCREEN_AND_POWER && it.timestamp in start..end }
        if (screenEvents.isEmpty()) return 0
        var total = 0L
        val first = screenEvents.first().type
        var onSince: Long? = if (first == SCREEN_NON_INTERACTIVE || first == DEVICE_SHUTDOWN) start else null
        for (e in screenEvents) {
            when (e.type) {
                SCREEN_INTERACTIVE -> if (onSince == null) onSince = e.timestamp
                SCREEN_NON_INTERACTIVE, DEVICE_SHUTDOWN -> {
                    onSince?.let { total += e.timestamp - it }
                    onSince = null
                }
                // During boot, "screen on" is logged within moments of the startup record, in either
                // order. Only an interval that began well before the startup (the device died without
                // a shutdown record) is dropped, because its real end is unknown.
                DEVICE_STARTUP -> if (onSince != null && onSince < e.timestamp - BOOT_TOLERANCE_MS) onSince = null
            }
        }
        onSince?.let { total += end - it }
        return total
    }

    private const val BOOT_TOLERANCE_MS = 60_000L
    private val SCREEN_AND_POWER = setOf(SCREEN_INTERACTIVE, SCREEN_NON_INTERACTIVE, DEVICE_SHUTDOWN, DEVICE_STARTUP)
}
