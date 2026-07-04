package io.github.matheusghenriques.thedoor.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_limits")

data class TimeSchedule(
    val daysOfWeek: List<Int>,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    val startMinOfDay: Int get() = startHour * 60 + startMinute
    val endMinOfDay: Int get() = endHour * 60 + endMinute
}

data class SavedTimeData(
    val dailyMinutes: Int = 0, val weekDays: List<Int> = emptyList(), val totalMinutes: Long = 0L
)

data class AppConfig(
    val blocked: Boolean = false,
    val schedules: List<TimeSchedule> = emptyList(),
    val perDayLimits: Map<Int, Int> = emptyMap(),
    val perDayTargets: Map<Int, Int> = emptyMap(),
    val targetMinutes: Int? = null,
    val lastReductionWeekStart: Long = 0L,
    val maxOpensPerDay: Int? = null
)

class AppLimitRepository(private val context: Context) {

    companion object {
        private val APP_CONFIGS = stringPreferencesKey("app_configs_json")
        private val REDIRECT_CONFIG = stringPreferencesKey("redirect_config_json")
        private val SAVED_TOTAL = longPreferencesKey("saved_total")
        private val SAVED_WEEK = stringPreferencesKey("saved_week")
        private val SAVED_LAST_DATE = stringPreferencesKey("saved_last_date")
        private val SAVED_PREV_DAILY = intPreferencesKey("saved_prev_daily")
    }

    val allConfigs: Flow<Map<String, AppConfig>> = context.dataStore.data.catch { e ->
        if (e is CorruptionException || e is IOException) {
            emit(emptyPreferences())
        } else throw e
    }.map { prefs ->
        parseConfigs(prefs[APP_CONFIGS] ?: "{}")
    }

    suspend fun saveConfig(packageName: String, config: AppConfig) {
        updateSavedTimes()
        context.dataStore.edit { prefs ->
            val current = parseConfigs(prefs[APP_CONFIGS] ?: "{}")
            prefs[APP_CONFIGS] = configsToJson(current + (packageName to config))
        }
    }

    val savedTimesFlow: Flow<SavedTimeData> = context.dataStore.data.catch { e ->
        if (e is CorruptionException || e is IOException) emit(emptyPreferences())
        else throw e
    }.map { prefs ->
        val configs = parseConfigs(prefs[APP_CONFIGS] ?: "{}")
        val daily = computeDailySaved(configs)
        val completedWeek = parseWeek(prefs[SAVED_WEEK] ?: "[]")
        val total = prefs[SAVED_TOTAL] ?: 0L
        SavedTimeData(
            dailyMinutes = daily,
            weekDays = (completedWeek.map { it.second } + daily).takeLast(7),
            totalMinutes = total + daily)
    }

    private fun computeDailySaved(configs: Map<String, AppConfig>): Int {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return configs.entries.sumOf { (pkg, cfg) ->
            val limit = cfg.perDayLimits[today] ?: return@sumOf 0
            val avg = getWeeklyAverageMinutes(pkg)
            maxOf(0, avg - limit)
        }
    }

    suspend fun updateSavedTimes() {
        context.dataStore.edit { prefs ->
            val configs = parseConfigs(prefs[APP_CONFIGS] ?: "{}")
            val daily = computeDailySaved(configs)
            val today = currentDateString()
            val lastDate = prefs[SAVED_LAST_DATE]

            if (lastDate != null && lastDate != today) {
                val prevDaily = prefs[SAVED_PREV_DAILY] ?: daily
                val total = prefs[SAVED_TOTAL] ?: 0L
                val week = parseWeek(prefs[SAVED_WEEK] ?: "[]")
                prefs[SAVED_TOTAL] = total + prevDaily
                prefs[SAVED_WEEK] = weekToJson((week + (lastDate to prevDaily)).takeLast(6))
            }

            prefs[SAVED_PREV_DAILY] = daily
            prefs[SAVED_LAST_DATE] = today
        }
    }

    fun getWeeklyAverageMinutes(packageName: String): Int {
        val usm =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0
        val calendar = Calendar.getInstance()
        val agora = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -28)
        val inicio = calendar.timeInMillis

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, inicio, agora) ?: return 0
        val appStats = stats.filter { it.packageName == packageName }
        if (appStats.isEmpty()) return 0

        val umDiaMs = 24L * 60 * 60 * 1000
        var somaPonderada = 0L
        var pesoTotal = 0

        for (stat in appStats) {
            val diasAtras = ((agora - stat.firstTimeStamp) / umDiaMs).toInt()
            val peso = when {
                diasAtras < 7 -> 4
                diasAtras < 14 -> 2
                else -> 1
            }
            somaPonderada += stat.totalTimeInForeground * peso
            pesoTotal += peso
        }

        if (pesoTotal == 0) return 0

        val mediaMin = (somaPonderada / pesoTotal / 60000).toInt()
        return if (mediaMin <= 0) 0 else ((mediaMin + 2) / 5) * 5
    }

    private val _refreshUsage = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun refreshUsage() {
        _refreshUsage.tryEmit(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val usageTodayFlow: Flow<Map<String, Long>> =
        _refreshUsage.onStart { emit(Unit) }.flatMapLatest {
                flow {
                    while (true) {
                        val usage = queryTodayUsage()
                        emit(usage)
                        delay(30_000L)
                    }
                }
            }.flowOn(Dispatchers.IO)

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

    val redirectConfig: Flow<RedirectConfig> = context.dataStore.data.catch { e ->
        if (e is CorruptionException || e is IOException) {
            emit(emptyPreferences())
        } else throw e
    }.map { prefs ->
        parseRedirectConfig(prefs[REDIRECT_CONFIG] ?: "{}")
    }

    suspend fun saveRedirectConfig(config: RedirectConfig) {
        context.dataStore.edit { prefs ->
            prefs[REDIRECT_CONFIG] = redirectConfigToJson(config)
        }
    }

    suspend fun applyGradualReductions() {
        val now = SystemClock.elapsedRealtime()
        val json = context.dataStore.data.first()[APP_CONFIGS] ?: return
        val configs = parseConfigs(json)
        var changed = false
        val updated = configs.mapValues { (_, config) ->
            if (config.perDayLimits.isNotEmpty() && config.perDayTargets.isNotEmpty()) {
                val reductionStart = if (config.lastReductionWeekStart > 99_999_999_999L) {
                    now // migrated from old wall-clock timestamp
                } else config.lastReductionWeekStart
                val weeksSince =
                    if (reductionStart > 0) (now - reductionStart) / (7L * 24 * 60 * 60 * 1000) else 0
                if (weeksSince < 1) {
                    if (reductionStart != config.lastReductionWeekStart) {
                        changed = true
                        config.copy(lastReductionWeekStart = now)
                    } else config
                } else {
                    val newLimits = config.perDayLimits.mapValues { (day, limit) ->
                        val target = config.perDayTargets[day] ?: return@mapValues limit
                        if (limit <= target) target
                        else {
                            val reduction =
                                ((limit * 0.10).toInt()).coerceIn(minOf(5, limit - target), 30)
                            maxOf(target, limit - reduction)
                        }
                    }
                    changed = true
                    config.copy(perDayLimits = newLimits, lastReductionWeekStart = now)
                }
            } else config
        }
        if (changed) {
            context.dataStore.edit { prefs -> prefs[APP_CONFIGS] = configsToJson(updated) }
        }
    }
}

private fun parseWeek(json: String): List<Pair<String, Int>> {
    val arr = JSONArray(json)
    return (0 until arr.length()).map { i ->
        val entry = arr.getJSONArray(i)
        entry.getString(0) to entry.getInt(1)
    }
}

private fun weekToJson(week: List<Pair<String, Int>>): String {
    val arr = JSONArray()
    week.forEach { (date, min) ->
        arr.put(JSONArray().apply { put(date); put(min) })
    }
    return arr.toString()
}

private fun currentDateString(): String {
    val c = Calendar.getInstance()
    return "%04d-%02d-%02d".format(
        c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
    )
}

fun configsToJson(configs: Map<String, AppConfig>): String {
    val obj = JSONObject()
    configs.forEach { (pkg, config) ->
        val c = JSONObject().apply {
            put("blocked", config.blocked)
            if (config.schedules.isNotEmpty()) {
                val sarr = JSONArray()
                config.schedules.forEach { s ->
                    sarr.put(JSONObject().apply {
                        put("days", JSONArray(s.daysOfWeek))
                        put("startH", s.startHour)
                        put("startM", s.startMinute)
                        put("endH", s.endHour)
                        put("endM", s.endMinute)
                    })
                }
                put("schedules", sarr)
            }
            if (config.perDayLimits.isNotEmpty()) {
                val pdObj = JSONObject()
                config.perDayLimits.forEach { (day, min) -> pdObj.put(day.toString(), min) }
                put("perDay", pdObj)
            }
            if (config.perDayTargets.isNotEmpty()) {
                val pdtObj = JSONObject()
                config.perDayTargets.forEach { (day, min) -> pdtObj.put(day.toString(), min) }
                put("perDayTarget", pdtObj)
            }
            config.maxOpensPerDay?.let { put("maxOpens", it) }
            put("lastReduction", config.lastReductionWeekStart)
        }
        obj.put(pkg, c)
    }
    return obj.toString()
}

fun parseConfigs(json: String): Map<String, AppConfig> {
    val obj = JSONObject(json)
    val result = mutableMapOf<String, AppConfig>()
    obj.keys().forEach { pkg ->
        val c = obj.getJSONObject(pkg)
        val schedules = if (c.has("schedules")) {
            val sarr = c.getJSONArray("schedules")
            (0 until sarr.length()).map { i ->
                val s = sarr.getJSONObject(i)
                val daysArr = s.getJSONArray("days")
                TimeSchedule(
                    daysOfWeek = (0 until daysArr.length()).map { daysArr.getInt(it) },
                    startHour = s.getInt("startH"),
                    startMinute = s.getInt("startM"),
                    endHour = s.getInt("endH"),
                    endMinute = s.getInt("endM")
                )
            }
        } else emptyList()
        val perDayLimits = if (c.has("perDay")) {
            val pdObj = c.getJSONObject("perDay")
            pdObj.keys().asSequence().associate { key -> key.toInt() to pdObj.getInt(key) }
        } else emptyMap()
        val perDayTargets = if (c.has("perDayTarget")) {
            val pdtObj = c.getJSONObject("perDayTarget")
            pdtObj.keys().asSequence().associate { key -> key.toInt() to pdtObj.getInt(key) }
        } else if (c.has("targetMin")) {
            perDayLimits.mapValues { c.getInt("targetMin") }
        } else emptyMap()
        result[pkg] = AppConfig(
            blocked = c.optBoolean("blocked"),
            schedules = schedules,
            perDayLimits = perDayLimits,
            perDayTargets = perDayTargets,
            maxOpensPerDay = if (c.has("maxOpens")) c.getInt("maxOpens") else null,
            lastReductionWeekStart = c.optLong("lastReduction")
        )
    }
    return result
}

fun redirectConfigToJson(config: RedirectConfig): String {
    val obj = JSONObject().apply {
        put("type", config.type.name)
        put("appPackage", config.appPackage)
        put("theDoorPhrase", config.theDoorPhrase)
        put("toastEnabled", config.toastEnabled)
        put("toastMessage", config.toastMessage)
    }
    return obj.toString()
}

fun parseRedirectConfig(json: String): RedirectConfig {
    val obj = JSONObject(json)
    return RedirectConfig(
        type = try {
            RedirectType.valueOf(obj.optString("type", "THE_DOOR"))
        } catch (_: Exception) {
            RedirectType.THE_DOOR
        },
        appPackage = obj.optString("appPackage", ""),
        theDoorPhrase = obj.optString("theDoorPhrase", ""),
        toastEnabled = obj.optBoolean("toastEnabled"),
        toastMessage = obj.optString("toastMessage", "")
    )

}
