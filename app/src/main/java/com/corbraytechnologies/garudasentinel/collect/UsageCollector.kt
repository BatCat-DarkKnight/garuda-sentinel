package com.corbraytechnologies.garudasentinel.collect

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.corbraytechnologies.garudasentinel.model.AppUsageInfo
import com.corbraytechnologies.garudasentinel.model.UsageSummary
import com.corbraytechnologies.garudasentinel.permissions.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * App and screen usage from UsageStatsManager. Requires the user to grant
 * Usage access in system settings. Android keeps detailed events for a limited
 * number of days, so this looks back 7 days at most.
 */
class UsageCollector(private val context: Context) {

    /** Returns null when Usage access has not been granted. */
    suspend fun collect(now: Long = System.currentTimeMillis()): UsageSummary? = withContext(Dispatchers.IO) {
        if (!Permissions.hasUsageAccess(context)) return@withContext null
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val pm = context.packageManager

        val todayStart = startOfDay(now)
        val windowStart = todayStart - 6 * DAY_MS

        val weekStats = usm.queryAndAggregateUsageStats(windowStart, now)
        val todayStats = usm.queryAndAggregateUsageStats(todayStart, now)
        val events = readEvents(usm, windowStart, now)

        val opens7d = UsageMath.countOpens(events)
        val opensToday = UsageMath.countOpens(events, since = todayStart)
        val lastUsed = UsageMath.lastUsed(events)
        val browsers = AppCollector.browserPackages(pm)

        // Opens are counted with the launcher still in the event stream, so leaving an app and
        // coming back through the home screen counts as a new open. The launcher itself is then
        // left out, because switching through it is not really "using" an app.
        val launcher = AppCollector.defaultLauncherPackage(pm)
        val packages = (weekStats.keys + opens7d.keys).filter { it != context.packageName && it != launcher }
        val apps = packages.mapNotNull { pkg ->
            val week = weekStats[pkg]?.totalTimeInForeground ?: 0L
            val opens = opens7d[pkg] ?: 0
            if (week == 0L && opens == 0) return@mapNotNull null
            AppUsageInfo(
                packageName = pkg,
                appName = labelOf(pm, pkg),
                foregroundTodayMs = todayStats[pkg]?.totalTimeInForeground ?: 0L,
                foregroundLast7DaysMs = week,
                opensToday = opensToday[pkg] ?: 0,
                opensLast7Days = opens,
                lastUsedAt = lastUsed[pkg] ?: weekStats[pkg]?.lastTimeUsed?.takeIf { it > 0 },
                isBrowser = pkg in browsers,
            )
        }.sortedByDescending { it.foregroundLast7DaysMs }

        UsageSummary(
            collectedAt = now,
            todayStart = todayStart,
            windowStart = windowStart,
            screenOnTodayMs = UsageMath.screenOnMillis(events, todayStart, now),
            apps = apps,
        )
    }

    private fun readEvents(usm: UsageStatsManager, start: Long, end: Long): List<UsageMath.Event> {
        val out = ArrayList<UsageMath.Event>()
        val events = usm.queryEvents(start, end) ?: return out
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                UsageMath.ACTIVITY_RESUMED, UsageMath.SCREEN_INTERACTIVE, UsageMath.SCREEN_NON_INTERACTIVE,
                UsageMath.DEVICE_SHUTDOWN, UsageMath.DEVICE_STARTUP ->
                    out += UsageMath.Event(e.packageName.orEmpty(), e.eventType, e.timeStamp)
            }
        }
        return out
    }

    private fun labelOf(pm: PackageManager, pkg: String): String = try {
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        pkg
    }

    private fun startOfDay(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
