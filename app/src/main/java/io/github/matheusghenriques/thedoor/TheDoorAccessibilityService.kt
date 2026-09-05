package io.github.matheusghenriques.thedoor

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.matheusghenriques.thedoor.data.AppConfig
import io.github.matheusghenriques.thedoor.data.AppLimitRepository
import io.github.matheusghenriques.thedoor.data.AppPreferences
import io.github.matheusghenriques.thedoor.data.CurrentUsageProvider
import io.github.matheusghenriques.thedoor.data.OpenCountProvider
import io.github.matheusghenriques.thedoor.data.PackageConstants
import io.github.matheusghenriques.thedoor.data.ProtectionConfig
import io.github.matheusghenriques.thedoor.data.RedirectConfig
import io.github.matheusghenriques.thedoor.data.RedirectType
import io.github.matheusghenriques.thedoor.data.TimeSchedule
import io.github.matheusghenriques.thedoor.data.UsageTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@SuppressLint("AccessibilityPolicy")
class TheDoorAccessibilityService : AccessibilityService() {

    companion object {
        const val EXTRA_REDIRECT_PHRASE = "redirect_phrase"
        private const val NOTIFICATION_CHANNEL_ID = "limit_warnings"
        private const val NOTIFICATION_CHANNEL_NAME = "Limit Warnings"
        private const val NOTIFICATION_ID_BASE = 1000
        private val BLOCKED_ACCESSIBILITY =
            listOf("accessibility", "acessibilidade", "aplicativos instalados", "installed apps")
        private val BLOCKED_DEV_OPTIONS =
            listOf("developer options", "opções do desenvolvedor", "opções de desenvolvedor")
        private val BLOCKED_VPN =
            listOf("vpn", "more connection settings", "mais configurações de conexão")
        private val BLOCKED_PRIVATE_DNS = listOf("private dns", "dns privado")
        private val BLOCKED_THE_DOOR = listOf("the door engine", "the door")
        private val BLOCKED_RETHINK = listOf("rethink")
        private val BLOCKED_LANGUAGE = listOf("language", "idioma")
        private val BLOCKED_ADMIN = listOf("admin", "administrador")
        private val BLOCKED_ADD_APPS_SECURE_FOLDER =
            listOf("AddAppsActivity", "add apps", "adicionar aplicativos")
        private val BLOCKED_UNKNOWN_INSTALL =
            listOf("install unknown apps", "instalar apps desconhecidos")
        private val BLOCKED_AUTO_BLOCKER =
            listOf("turn off auto blocker", "desativar o bloqueador automático")

    }

    private val userBlockedApps = ConcurrentHashMap<String, Boolean>()
    private val appLimitsMs = ConcurrentHashMap<String, Long>()
    private val scheduledApps = ConcurrentHashMap<String, List<TimeSchedule>>()
    private val maxOpensMap = ConcurrentHashMap<String, Int>()
    private val openCounts = ConcurrentHashMap<String, Int>()

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var tracker: UsageTracker
    private lateinit var repo: AppLimitRepository

    private var lastActiveApp: String? = null
    private var trackedNotificationDate: String = ""
    private val notifiedThresholds = ConcurrentHashMap<String, Int>()
    private val appNameCache = ConcurrentHashMap<String, String>()

    @Volatile
    private var cachedRedirectConfig = RedirectConfig()

    @Volatile
    private var cachedConfigs: Map<String, AppConfig> = emptyMap()

    @Volatile
    private var setupComplete = false
    private var trackedOpensDate: String = ""

    private var lastTimeChangeCheck: Long = 0L

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                tracker.onScreenOff()
            }
        }
    }

    private val protectionConfig = AtomicReference(ProtectionConfig())
    private val appPrefs by lazy { AppPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        trackedNotificationDate = currentDateString()
        repo = AppLimitRepository(this)
        tracker = UsageTracker(this)

        scope.launch { watchConfigChanges() }
        scope.launch { watchRedirectConfig() }
        scope.launch {
            appPrefs.onboardingComplete.collect { complete ->
                setupComplete = complete
            }
        }
        scope.launch {
            appPrefs.protectionConfig.collect { config ->
                protectionConfig.set(config)
            }
        }

        handler.postDelayed(gradualReductionCheck, 6 * 60 * 60 * 1000L)
        handler.postDelayed(limitCheckRunnable, 5_000L)

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOffReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(screenOffReceiver, filter)
        }
    }

    private suspend fun watchConfigChanges() {
        repo.allConfigs.collect { configs ->
            cachedConfigs = configs
            userBlockedApps.clear()
            userBlockedApps.putAll(configs.filter { it.value.blocked }.keys.associateWith { true })
            appLimitsMs.clear()
            recalcAppLimitsMs()
            scheduledApps.clear()
            scheduledApps.putAll(configs.mapValues { (_, cfg) -> cfg.schedules }
                .filter { it.value.isNotEmpty() })
            maxOpensMap.clear()
            maxOpensMap.putAll(configs.mapValues { it.value.maxOpensPerDay }
                .filter { it.value != null }.mapValues { it.value!! })
            val today = currentDateString()
            if (trackedOpensDate != today) {
                openCounts.clear()
                trackedOpensDate = today
            }
            OpenCountProvider.openCounts.value = openCounts.toMap()
        }
    }

    private fun recalcAppLimitsMs() {
        val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        appLimitsMs.clear()
        cachedConfigs.forEach { (pkg, cfg) ->
            val limitMin =
                cfg.perDayLimits[todayDow] ?: cfg.perDayLimits.values.maxOrNull() ?: return@forEach
            appLimitsMs[pkg] = limitMin * 60 * 1000L
        }
    }

    private suspend fun watchRedirectConfig() {
        repo.redirectConfig.collect { config ->
            cachedRedirectConfig = config
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(gradualReductionCheck)
        handler.removeCallbacks(limitCheckRunnable)
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {
        }
    }

    private val gradualReductionCheck = object : Runnable {
        override fun run() {
            scope.launch { repo.applyGradualReductions() }
            handler.postDelayed(this, 6 * 60 * 60 * 1000L)
        }
    }

    private val limitCheckRunnable = object : Runnable {
        override fun run() {
            val tcc = TimeChangeReceiver.lastTimeChangeAt
            if (tcc > lastTimeChangeCheck) {
                lastTimeChangeCheck = tcc
                recalcAppLimitsMs()
                openCounts.clear()
                OpenCountProvider.openCounts.value = openCounts.toMap()
                notifiedThresholds.clear()
                trackedNotificationDate = currentDateString()
            }

            val today = currentDateString()
            if (trackedNotificationDate != today) {
                notifiedThresholds.clear()
                trackedNotificationDate = today
                recalcAppLimitsMs()
                openCounts.clear()
                OpenCountProvider.openCounts.value = openCounts.toMap()
                tracker.onDayReset()
            }
            val nextDelay = lastActiveApp?.let { app ->
                appLimitsMs[app]?.let { limitMs ->
                    val usage = tracker.getCurrentUsage(app)
                    val remaining = limitMs - usage
                    if (remaining <= 0) {
                        triggerBlock()
                    } else {
                        val thresholds = listOf(900_000, 300_000, 60_000)
                        for (threshold in thresholds) {
                            if (threshold in remaining..<limitMs) {
                                val notified = notifiedThresholds[app] ?: Int.MAX_VALUE
                                if (threshold < notified) {
                                    showLimitWarning(app, threshold)
                                    notifiedThresholds[app] = threshold
                                }
                            }
                        }
                    }
                    val delay = when {
                        remaining > 915_000L -> remaining - 915_000L
                        remaining > 900_000L -> 20_000L
                        remaining > 315_000L -> remaining - 315_000L
                        remaining > 300_000L -> 20_000L
                        remaining > 75_000L -> remaining - 75_000L
                        remaining > 60_000L -> 20_000L
                        remaining > 20_000L -> 40_000L.coerceAtMost(remaining - 5_000L)
                        else -> remaining + 5_000L
                    }
                    delay
                }
            } ?: 300_000L
            if (appLimitsMs.isNotEmpty()) {
                CurrentUsageProvider.currentUsage.value = tracker.getAllUsage(appLimitsMs.keys)
            }
            tracker.save()
            handler.postDelayed(this, nextDelay)
        }
    }

    private fun triggerBlock() {
        performGlobalAction(GLOBAL_ACTION_BACK)

        handler.postDelayed({
            val config = cachedRedirectConfig

            if (config.type != RedirectType.NONE) {
                val targetPkg = when (config.type) {
                    RedirectType.APP -> config.appPackage
                    RedirectType.THE_DOOR -> packageName
                }

                if (targetPkg.isNotBlank() && !userBlockedApps.containsKey(targetPkg) && !appLimitsMs.containsKey(
                        targetPkg
                    ) && !isScheduleActive(targetPkg)
                ) {
                    val intent = if (config.type == RedirectType.APP) {
                        packageManager.getLaunchIntentForPackage(config.appPackage)
                    } else {
                        packageManager.getLaunchIntentForPackage(packageName)
                    }
                    if (intent != null) {
                        if (config.type == RedirectType.THE_DOOR && config.theDoorPhrase.isNotBlank()) {
                            intent.putExtra(EXTRA_REDIRECT_PHRASE, config.theDoorPhrase)
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                    }
                }
            } else performGlobalAction(GLOBAL_ACTION_HOME)

            if (config.toastEnabled && config.toastMessage.isNotBlank()) {
                Toast.makeText(applicationContext, config.toastMessage, Toast.LENGTH_SHORT).show()
            }
        }, 30L)
    }

    private fun isScheduleActive(packageName: String): Boolean {
        val schedules = scheduledApps[packageName] ?: return false
        val now = Calendar.getInstance()
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        val currentMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return schedules.any { schedule ->
            dayOfWeek in schedule.daysOfWeek && currentMin >= schedule.startMinOfDay && currentMin < schedule.endMinOfDay
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        if (!setupComplete) return

        val packageName = event.packageName?.toString() ?: return

        Log.d(
            "DoorBug",
            "event type=${event.eventType} pkg=$packageName cls=${event.className} text=\"${
                event.text.joinToString(" ")
            }\" contentDesc=\"${event.contentDescription}\""
        )

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val contentDesc = event.contentDescription?.toString()?.lowercase() ?: ""
            if (protectionConfig.get().blockPowerMenu && packageName == PackageConstants.SYSTEM_UI && contentDesc in setOf(
                    "power off,", "desligar,"
                )
            ) {
                triggerBlock()
                return
            }
        }

        val className = event.className?.toString() ?: ""

        if (protectionConfig.get().blockPowerMenu && packageName == PackageConstants.SYSTEM_UI && className.contains(
                "SamsungGlobalActionsDialog"
            )
        ) {
            val dialogText = event.text.joinToString(" ").lowercase()
            if (dialogText.contains("side button settings") || dialogText.contains("configurações do botão lateral")) {
                lockScreenViaAdmin()
                return
            }
        }

        if (packageName == PackageConstants.SETTINGS) {
            val cfg = protectionConfig.get()
            val eventText = event.text.joinToString(" ").lowercase()
            val blockedKeywords = buildList {
                if (cfg.blockAccessibilitySettings) addAll(BLOCKED_ACCESSIBILITY)
                if (cfg.blockDeveloperOptions) addAll(BLOCKED_DEV_OPTIONS)
                if (cfg.blockVpn) addAll(BLOCKED_VPN)
                if (cfg.blockPrivateDns) addAll(BLOCKED_PRIVATE_DNS)
                if (cfg.blockUninstallTheDoor) addAll(BLOCKED_THE_DOOR)
                if (cfg.blockUninstallRethink) addAll(BLOCKED_RETHINK)
                if (cfg.blockLanguageChanges) addAll(BLOCKED_LANGUAGE)
                if (cfg.blockAppInfo) addAll(BLOCKED_ADMIN)
                if (cfg.blockInstallUnknownApps) addAll(BLOCKED_UNKNOWN_INSTALL)
            }
            val isAppInfoScreen =
                cfg.blockAppInfo && (className.contains("AppInfoDashboardActivity") || className.contains(
                    "InstalledAppDetailsActivity"
                ) || eventText.contains("app info") || eventText.contains("informações do aplicativo") || eventText.contains(
                    "info do app"
                ))
            if (blockedKeywords.any { eventText.contains(it) } || (cfg.blockAppInfo && className.contains(
                    "DeviceAdminSettingsActivity"
                )) || isAppInfoScreen) {
                triggerBlock()
                return
            }
        }

        if (packageName == PackageConstants.SAMSUNG_BIOMETRICS_SETTINGS) {
            val cfg = protectionConfig.get()
            val eventText = event.text.joinToString(" ").lowercase()
            if (cfg.blockAutoBlocker && BLOCKED_AUTO_BLOCKER.any { eventText.contains(it) }) {
                triggerBlock()
                return
            }
        }

        val eventText = event.text.joinToString(" ").lowercase()
        if (packageName == PackageConstants.SECURE_FOLDER && protectionConfig.get().blockSecureFolderAddApps && BLOCKED_ADD_APPS_SECURE_FOLDER.any {
                className.contains(
                    it
                ) || eventText.contains(it)
            }) {
            triggerBlock()
            return
        }

        val isNewApp = lastActiveApp != packageName

        if (isNewApp) {
            trackAppUsage(packageName)
        }

        if (userBlockedApps.containsKey(packageName)) {
            triggerBlock()
            return
        }

        if (isScheduleActive(packageName)) {
            triggerBlock()
            return
        }

        if (isNewApp && (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)) {
            maxOpensMap[packageName]?.let { maxOpens ->
                val count = (openCounts[packageName] ?: 0) + 1
                openCounts[packageName] = count
                OpenCountProvider.openCounts.value = openCounts.toMap()
                if (count > maxOpens) {
                    triggerBlock()
                    return
                }
            }
        }

        appLimitsMs[packageName]?.let { limitMs ->
            if (isNewApp && tracker.getCurrentUsage(packageName) >= limitMs) {
                triggerBlock()
                return
            }
        }

        if (packageName == PackageConstants.PACKAGE_INSTALLER_GOOGLE || packageName == PackageConstants.PACKAGE_INSTALLER_AOSP) {
            val cfg = protectionConfig.get()
            val eventText = event.text.joinToString(" ").lowercase()
            if (eventText.contains("uninstall") || eventText.contains("desinstalar")) {
                if (cfg.blockUninstallTheDoor && eventText.contains("the door")) {
                    triggerBlock()
                    return
                }
                if (cfg.blockUninstallFirefox && eventText.contains("firefox")) {
                    triggerBlock()
                    return
                }
                if (cfg.blockUninstallRethink && eventText.contains("rethink")) {
                    triggerBlock()
                    return
                }
            }
        }

        if (packageName == PackageConstants.FIREFOX) {
            val cfg = protectionConfig.get()
            val eventText = event.text.joinToString(" ").lowercase()
            if (cfg.blockFirefoxSettings && (eventText.contains("extensions") || eventText.contains(
                    "settings"
                ) || eventText.contains("extensões") || eventText.contains("configurações"))
            ) {
                triggerBlock()
                return
            }
            if (cfg.blockFirefoxUblockOrigin && eventText.contains("ublock origin")) {
                triggerBlock()
                return
            }
            if (cfg.blockFirefoxBlockNSFW && eventText.contains("blocknsfw")) {
                triggerBlock()
                return
            }
        }
    }

    private var lastSideButtonLockAt = 0L

    private fun lockScreenViaAdmin() {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastSideButtonLockAt < 3000L) {
            Log.d("DoorBug", "SideBtn lock skipped by debounce")
            return
        }
        lastSideButtonLockAt = now
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(this, TheDoorAdminReceiver::class.java)
            val active = dpm.isAdminActive(admin)
            Log.d("DoorBug", "SideBtn adminActive=$active")
            if (active) {
                dpm.lockNow()
                Log.d("DoorBug", "SideBtn lockNow() called")
            }
        } catch (e: Exception) {
            Log.d("DoorBug", "SideBtn lock failed: ${e::class.java.simpleName} ${e.message}")
        }
    }

    private fun trackAppUsage(packageName: String) {
        tracker.onAppOpened(packageName)
        lastActiveApp = packageName
        if (appLimitsMs.containsKey(packageName)) {
            handler.removeCallbacks(limitCheckRunnable)
            handler.post(limitCheckRunnable)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun showLimitWarning(app: String, thresholdMs: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val minutes = thresholdMs / 60_000
        val appName = getAppName(app)

        val message = when (minutes) {
            1 -> getString(R.string.limit_warning_1min, appName)
            5 -> getString(R.string.limit_warning_5min, appName)
            else -> getString(R.string.limit_warning_15min, appName)
        }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.the_door_notification)
            .setContentTitle(getString(R.string.limit_warning_title, appName))
            .setContentText(message).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true).build()

        NotificationManagerCompat.from(this)
            .notify(NOTIFICATION_ID_BASE + app.hashCode(), notification)
    }

    private fun getAppName(packageName: String): String {
        return appNameCache.getOrPut(packageName) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                packageName
            }
        }
    }

    private fun currentDateString(): String {
        val cal = Calendar.getInstance()
        return String.format(
            Locale.getDefault(),
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    override fun onInterrupt() {
        handler.removeCallbacks(gradualReductionCheck)
        handler.removeCallbacks(limitCheckRunnable)
    }
}
