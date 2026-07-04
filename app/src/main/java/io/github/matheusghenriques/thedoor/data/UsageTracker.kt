package io.github.matheusghenriques.thedoor.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.Calendar

private val Context.trackingStore: DataStore<Preferences> by preferencesDataStore(name = "tracking")

class UsageTracker(private val context: Context) {

    private var currentApp: String? = null
    private var sessionStart: Long = 0L
    private val accumulated = mutableMapOf<String, Long>()
    private var trackedDate: String = ""

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private val KEY_TRACKING_DATE = stringPreferencesKey("tracking_date")
        private val KEY_TRACKING_USAGE = stringPreferencesKey("tracking_usage")
    }

    init {
        runBlocking(Dispatchers.IO) {
            val prefs = context.trackingStore.data.first()
            val savedDate = prefs[KEY_TRACKING_DATE] ?: return@runBlocking
            val savedUsage = prefs[KEY_TRACKING_USAGE] ?: return@runBlocking
            val today = currentDateString()
            if (savedDate == today) {
                trackedDate = savedDate
                try {
                    val json = JSONObject(savedUsage)
                    json.keys().forEach { key ->
                        accumulated[key] = json.getLong(key)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun closeSession() {
        if (currentApp != null && sessionStart > 0) {
            val elapsed = SystemClock.elapsedRealtime() - sessionStart
            accumulated[currentApp!!] = (accumulated[currentApp!!] ?: 0L) + elapsed
        }
    }

    fun onAppOpened(packageName: String) {
        closeSession()

        val today = currentDateString()
        if (trackedDate != today) {
            accumulated.clear()
            trackedDate = today
        }

        currentApp = packageName
        sessionStart = SystemClock.elapsedRealtime()

        persistState()
    }

    fun onScreenOff() {
        closeSession()
        currentApp = null
        sessionStart = 0L
        persistState()
    }

    fun save() {
        persistState()
    }

    private fun queryTodayUsage(): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val midnight = cal.timeInMillis
        val end = midnight + 86400000L

        return queryFromEvents(usm, midnight, end)
    }

    private fun queryFromEvents(
        usm: UsageStatsManager, midnight: Long, end: Long
    ): Map<String, Long> {
        val events = usm.queryEvents(midnight, end) ?: return emptyMap()
        val totals = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()
        var currentPkg: String? = null
        var currentStart = 0L

        while (events.hasNextEvent()) {
            try {
                events.getNextEvent(event)
            } catch (_: Exception) {
                break
            }
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (currentPkg != null && currentStart > 0 && event.timeStamp > currentStart) {
                        totals[currentPkg] =
                            (totals[currentPkg] ?: 0L) + (event.timeStamp - currentStart)
                    }
                    currentPkg = event.packageName
                    currentStart = event.timeStamp
                }

                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (currentPkg == event.packageName && currentStart > 0 && event.timeStamp > currentStart) {
                        totals[event.packageName] =
                            (totals[event.packageName] ?: 0L) + (event.timeStamp - currentStart)
                    }
                    currentPkg = null
                    currentStart = 0L
                }
            }
        }

        if (totals.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return usm.queryAndAggregateUsageStats(midnight, end)
                .mapValues { it.value.totalTimeInForeground }
        }
        return totals
    }

    fun getCurrentUsage(packageName: String): Long {
        val acc = accumulated[packageName] ?: 0L
        val session =
            if (currentApp == packageName && sessionStart > 0) SystemClock.elapsedRealtime() - sessionStart else 0L
        val trackerMs = acc + session
        val systemMs = queryTodayUsage()[packageName] ?: 0L
        return maxOf(trackerMs, systemMs)
    }

    fun getAllUsage(packages: Set<String>): Map<String, Long> {
        val now = SystemClock.elapsedRealtime()
        val todayUsage = queryTodayUsage()
        return packages.associateWith { pkg ->
            val trackerMs = (accumulated[pkg]
                ?: 0L) + if (currentApp == pkg && sessionStart > 0) now - sessionStart else 0L
            maxOf(trackerMs, todayUsage[pkg] ?: 0L)
        }
    }

    fun onDayReset() {
        accumulated.clear()
        trackedDate = currentDateString()
        currentApp = null
        sessionStart = 0L
        persistState()
    }

    private fun persistState() {
        scope.launch {
            context.trackingStore.edit { prefs ->
                prefs[KEY_TRACKING_DATE] = trackedDate
                prefs[KEY_TRACKING_USAGE] = JSONObject(accumulated.toMap()).toString()
            }
        }
    }

    fun getWeeklyAverageMinutes(packageName: String): Int? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -28)
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, now
        )?.filter { it.packageName == packageName } ?: return 0

        val dayTotals = mutableMapOf<Long, Long>()
        for (stat in stats) {
            val dc = Calendar.getInstance().apply { timeInMillis = stat.firstTimeStamp }
            dc.set(Calendar.HOUR_OF_DAY, 0); dc.set(Calendar.MINUTE, 0)
            dc.set(Calendar.SECOND, 0); dc.set(Calendar.MILLISECOND, 0)
            dayTotals[dc.timeInMillis] =
                (dayTotals[dc.timeInMillis] ?: 0L) + stat.totalTimeInForeground
        }

        var weightedSum = 0L
        var totalWeight = 0
        for ((dayStart, ms) in dayTotals) {
            val daysAgo = ((now - dayStart) / 86400000L).toInt()
            if (daysAgo !in 0..<28) continue
            val weekBucket = daysAgo / 7
            val weight = 4 - weekBucket
            weightedSum += ms * weight
            totalWeight += weight
        }
        if (totalWeight == 0) return null
        return ((weightedSum / totalWeight) / 60000).toInt().coerceAtLeast(0)
    }

    private fun currentDateString(): String {
        val c = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }
}
