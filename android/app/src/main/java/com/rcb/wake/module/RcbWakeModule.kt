package com.rcb.wake.module

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.OpenableColumns
import android.provider.Settings
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.bridge.WritableMap
import com.rcb.wake.BuildConfig
import com.rcb.wake.R
import com.rcb.wake.alarm.Alarm
import com.rcb.wake.alarm.AlarmController
import com.rcb.wake.alarm.AlarmInput
import com.rcb.wake.alarm.AlarmSessionStore
import com.rcb.wake.alarm.InvalidAlarmException
import com.rcb.wake.alarm.ScheduleState
import com.rcb.wake.audio.AlarmPlayer
import com.rcb.wake.audio.AlarmSounds
import com.rcb.wake.module.specs.NativeRcbWakeSpec
import com.rcb.wake.notification.WakeNotifications
import com.rcb.wake.service.RingingService
import com.rcb.wake.storage.AppStorage
import com.rcb.wake.storage.SettingsStore
import com.rcb.wake.storage.WakeSettings
import com.rcb.wake.storage.WorldClock
import com.rcb.wake.storage.WorldClockRepository
import com.rcb.wake.timer.TimerController
import com.rcb.wake.timer.WakeTimer
import com.rcb.wake.weather.WeatherRepository
import com.rcb.wake.weather.WeatherSnapshot
import com.rcb.wake.widget.ClockWidgetProvider
import java.time.Instant
import java.time.ZoneId

/**
 * React Native entry point to the native engine. Every method validates its input in Kotlin and
 * delegates to the same controllers used by receivers and the ringing service.
 */
class RcbWakeModule(private val context: ReactApplicationContext) : NativeRcbWakeSpec(context) {
    private val alarms by lazy { AlarmController(context) }
    private val timers by lazy { TimerController(context) }
    private val clocks by lazy { WorldClockRepository(context) }
    private var previewPlayer: AlarmPlayer? = null
    private var pendingSoundPick: Promise? = null

    private val engineListener = EngineEvents.Listener { type ->
        if (type == EngineEvents.SESSION_ENDED) releaseLockScreenDisplay()
        emitOnEngineEvent(Arguments.createMap().apply { putString("type", type) })
    }

    private val activityListener = object : BaseActivityEventListener() {
        override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode != REQUEST_PICK_SOUND) return
            val promise = pendingSoundPick ?: return
            pendingSoundPick = null
            val uri = data?.data
            if (resultCode != Activity.RESULT_OK || uri == null) {
                promise.resolve(null)
                return
            }
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                promise.resolve(soundMap(uri.toString(), displayName(uri)))
            } catch (e: SecurityException) {
                promise.reject("E_SOUND_PERMISSION", "Could not keep access to the selected file", e)
            }
        }
    }

    init {
        EngineEvents.addListener(engineListener)
        context.addActivityEventListener(activityListener)
    }

    override fun invalidate() {
        EngineEvents.removeListener(engineListener)
        context.removeActivityEventListener(activityListener)
        UiThreadUtil.runOnUiThread { previewPlayer?.stop() }
        super.invalidate()
    }

    override fun getName() = NAME

    private inline fun guard(promise: Promise, block: () -> Unit) {
        try {
            block()
        } catch (e: InvalidAlarmException) {
            promise.reject("E_INVALID", e.message, e)
        } catch (e: IllegalArgumentException) {
            promise.reject("E_INVALID", e.message, e)
        } catch (e: Exception) {
            promise.reject("E_ENGINE", e.message, e)
        }
    }

    // ---- Engine lifecycle and status ----

    override fun initializeEngine(promise: Promise) = guard(promise) {
        WakeNotifications.ensureChannels(context)
        alarms.reconcileAll("app-start")
        timers.reconcileAll()
        promise.resolve(status())
    }

    override fun getAlarmEngineStatus(promise: Promise) = guard(promise) { promise.resolve(status()) }

    private fun status(): WritableMap {
        val all = alarms.getAll()
        val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
        return Arguments.createMap().apply {
            putBoolean("exactAlarmPermitted", alarms.canScheduleExact())
            putBoolean("notificationsPermitted", WakeNotifications.canPostNotifications(context))
            putBoolean("fullScreenIntentPermitted", WakeNotifications.canUseFullScreenIntent(context))
            putInt("sdkInt", Build.VERSION.SDK_INT)
            putString("versionName", pkg.versionName ?: BuildConfig.VERSION_NAME)
            putInt("versionCode", BuildConfig.VERSION_CODE)
            putInt("scheduledCount", all.count { it.scheduleState == ScheduleState.EXACT || it.scheduleState == ScheduleState.INEXACT })
            putInt("failedCount", all.count { it.scheduleState == ScheduleState.FAILED })
        }
    }

    override fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        startSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri()))
    }

    override fun requestFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT < 34) return
        startSettings(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri()))
    }

    override fun openNotificationSettings() {
        startSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }

    private fun packageUri() = Uri.parse("package:${context.packageName}")

    private fun startSettings(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
    }

    // ---- Alarms ----

    override fun getAlarms(promise: Promise) = guard(promise) {
        promise.resolve(Arguments.createArray().apply { alarms.getAll().forEach { pushMap(alarmMap(it)) } })
    }

    override fun saveAlarm(config: ReadableMap, promise: Promise) = guard(promise) {
        val id = config.getDouble("id").toLong()
        val input = AlarmInput(
            id = if (id == 0L) null else id,
            hour = config.getDouble("hour").toInt(),
            minute = config.getDouble("minute").toInt(),
            enabled = config.getBoolean("enabled"),
            repeatDays = config.getDouble("repeatDays").toInt(),
            label = config.getString("label").orEmpty(),
            soundUri = config.getString("soundUri").orEmpty(),
            soundName = config.getString("soundName").orEmpty(),
            vibrationEnabled = config.getBoolean("vibrationEnabled"),
            gradualVolumeEnabled = config.getBoolean("gradualVolumeEnabled"),
            snoozeMinutes = config.getDouble("snoozeMinutes").toInt(),
            challengeEnabled = config.getBoolean("challengeEnabled"),
            challengeDifficulty = config.getString("challengeDifficulty").orEmpty(),
            challengeQuestionCount = config.getDouble("challengeQuestionCount").toInt(),
        )
        promise.resolve(alarmMap(alarms.save(input)))
    }

    override fun setAlarmEnabled(alarmId: Double, enabled: Boolean, promise: Promise) = guard(promise) {
        promise.resolve(alarmMap(alarms.setEnabled(alarmId.toLong(), enabled)))
    }

    override fun deleteAlarm(alarmId: Double, promise: Promise) = guard(promise) {
        alarms.delete(alarmId.toLong())
        promise.resolve(null)
    }

    override fun cancelAllAlarms(promise: Promise) = guard(promise) {
        alarms.cancelAll()
        promise.resolve(null)
    }

    override fun getScheduledAlarms(promise: Promise) = guard(promise) {
        val result = Arguments.createArray()
        alarms.getAll().filter { it.nextTriggerAt != null }.forEach {
            result.pushMap(
                Arguments.createMap().apply {
                    putDouble("alarmId", it.id.toDouble())
                    putDouble("triggerAt", it.nextTriggerAt!!.toDouble())
                    putString("scheduleState", it.scheduleState.name)
                    putBoolean("registered", alarms.isRegistered(it.id))
                },
            )
        }
        promise.resolve(result)
    }

    override fun getNextAlarm(promise: Promise) = guard(promise) {
        promise.resolve(alarms.nextAlarm()?.let(::alarmMap))
    }

    private fun alarmMap(a: Alarm): WritableMap = Arguments.createMap().apply {
        putDouble("id", a.id.toDouble())
        putInt("hour", a.hour)
        putInt("minute", a.minute)
        putBoolean("enabled", a.enabled)
        putInt("repeatDays", a.repeatDays)
        putString("label", a.label)
        putString("soundUri", a.soundUri)
        putString("soundName", a.soundName)
        putBoolean("vibrationEnabled", a.vibrationEnabled)
        putBoolean("gradualVolumeEnabled", a.gradualVolumeEnabled)
        putInt("snoozeMinutes", a.snoozeMinutes)
        putBoolean("challengeEnabled", a.challengeEnabled)
        putString("challengeDifficulty", a.challengeDifficulty)
        putInt("challengeQuestionCount", a.challengeQuestionCount)
        putDouble("createdAt", a.createdAt.toDouble())
        putDouble("updatedAt", a.updatedAt.toDouble())
        putDouble("snoozedUntil", (a.snoozedUntil ?: 0L).toDouble())
        putDouble("nextTriggerAt", (a.nextTriggerAt ?: 0L).toDouble())
        putString("scheduleState", a.scheduleState.name)
    }

    // ---- Ringing session ----

    override fun getActiveSession(promise: Promise) = guard(promise) {
        val session = AlarmSessionStore.load(context)
        if (session == null) {
            promise.resolve(null)
            return@guard
        }
        promise.resolve(
            Arguments.createMap().apply {
                putArray("alarmIds", Arguments.createArray().apply { session.alarmIds.forEach { pushDouble(it.toDouble()) } })
                putArray(
                    "labels",
                    Arguments.createArray().apply { session.alarmIds.mapNotNull { alarms.get(it)?.label }.forEach { pushString(it) } },
                )
                putDouble("startedAt", session.startedAt.toDouble())
                putBoolean("challengeRequired", session.challengeRequired)
                putString("challengeDifficulty", session.challengeDifficulty)
                putInt("challengeQuestionCount", session.challengeQuestionCount)
            },
        )
    }

    override fun snoozeSession(promise: Promise) = guard(promise) {
        if (AlarmSessionStore.load(context) == null) {
            promise.resolve(0.0)
            return@guard
        }
        context.startService(RingingService.snoozeIntent(context))
        promise.resolve(1.0)
    }

    override fun dismissSession(method: String, promise: Promise) = guard(promise) {
        // The service still refuses METHOD_NORMAL while a challenge is required.
        require(method in setOf(RingingService.METHOD_NORMAL, RingingService.METHOD_CHALLENGE, RingingService.METHOD_EMERGENCY)) {
            "Unknown dismiss method"
        }
        if (AlarmSessionStore.load(context) == null) {
            promise.resolve(false)
            return@guard
        }
        context.startService(RingingService.dismissIntent(context, method))
        promise.resolve(true)
    }

    /** MainActivity is allowed over the lock screen only while a challenge is being solved. */
    private fun releaseLockScreenDisplay() {
        val activity = context.currentActivity ?: return
        UiThreadUtil.runOnUiThread {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(false)
                activity.setTurnScreenOn(false)
            }
        }
    }

    // ---- Sounds and pickers ----

    override fun getBuiltInSounds(promise: Promise) = guard(promise) {
        val result = Arguments.createArray()
        AlarmSounds.BUILT_IN.forEach { result.pushMap(soundMap("builtin:${it.key}", it.name)) }
        result.pushMap(soundMap(AlarmSounds.SYSTEM_DEFAULT_URI, "System default"))
        promise.resolve(result)
    }

    override fun pickCustomSound(promise: Promise) {
        val activity = context.currentActivity
        if (activity == null) {
            promise.reject("E_NO_ACTIVITY", "No foreground activity")
            return
        }
        pendingSoundPick?.resolve(null)
        pendingSoundPick = promise
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("audio/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        activity.startActivityForResult(intent, REQUEST_PICK_SOUND)
    }

    private fun displayName(uri: Uri): String =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }?.substringBeforeLast('.') ?: "Custom sound"

    private fun soundMap(uri: String, name: String) = Arguments.createMap().apply {
        putString("uri", uri)
        putString("name", name)
    }

    override fun previewSound(uri: String) {
        if (!AlarmSounds.isSupportedUri(uri)) return
        UiThreadUtil.runOnUiThread {
            val player = previewPlayer ?: AlarmPlayer(context).also { previewPlayer = it }
            player.start(uri, gradualVolume = false)
        }
    }

    override fun stopPreview() {
        UiThreadUtil.runOnUiThread { previewPlayer?.stop() }
    }

    override fun showTimePicker(hour: Double, minute: Double, is24Hour: Boolean, promise: Promise) {
        val activity = context.currentActivity
        if (activity == null) {
            promise.reject("E_NO_ACTIVITY", "No foreground activity")
            return
        }
        val pink = SettingsStore.load(context).theme == SettingsStore.THEME_PINK
        UiThreadUtil.runOnUiThread {
            var settled = false
            val dialog = TimePickerDialog(
                activity,
                if (pink) R.style.TimePickerPink else R.style.TimePickerMidnight,
                { _, h, m ->
                    settled = true
                    promise.resolve(pickedTime(h, m, false))
                },
                hour.toInt().coerceIn(0, 23),
                minute.toInt().coerceIn(0, 59),
                is24Hour,
            )
            dialog.setOnDismissListener {
                if (!settled) {
                    settled = true
                    promise.resolve(pickedTime(hour.toInt(), minute.toInt(), true))
                }
            }
            dialog.show()
        }
    }

    private fun pickedTime(h: Int, m: Int, cancelled: Boolean) = Arguments.createMap().apply {
        putInt("hour", h)
        putInt("minute", m)
        putBoolean("cancelled", cancelled)
    }

    // ---- Timers ----

    override fun getTimers(promise: Promise) = guard(promise) {
        promise.resolve(Arguments.createArray().apply { timers.getAll().forEach { pushMap(timerMap(it)) } })
    }

    override fun createTimer(label: String, durationMillis: Double, start: Boolean, promise: Promise) = guard(promise) {
        promise.resolve(timerMap(timers.create(label, durationMillis.toLong(), start)))
    }

    override fun startTimer(timerId: Double, promise: Promise) = guard(promise) {
        promise.resolve(timerMap(timers.start(timerId.toLong())))
    }

    override fun pauseTimer(timerId: Double, promise: Promise) = guard(promise) {
        promise.resolve(timerMap(timers.pause(timerId.toLong())))
    }

    override fun resetTimer(timerId: Double, promise: Promise) = guard(promise) {
        promise.resolve(timers.reset(timerId.toLong())?.let(::timerMap))
    }

    override fun addTimerTime(timerId: Double, millis: Double, promise: Promise) = guard(promise) {
        promise.resolve(timerMap(timers.addTime(timerId.toLong(), millis.toLong())))
    }

    override fun deleteTimer(timerId: Double, promise: Promise) = guard(promise) {
        timers.delete(timerId.toLong())
        promise.resolve(null)
    }

    private fun timerMap(t: WakeTimer) = Arguments.createMap().apply {
        putDouble("id", t.id.toDouble())
        putString("label", t.label)
        putDouble("durationMillis", t.durationMillis.toDouble())
        putDouble("remainingMillis", t.remainingMillis.toDouble())
        putDouble("endElapsedMillis", (t.endElapsedMillis ?: 0L).toDouble())
        putString("status", t.status.name)
        putDouble("createdAt", t.createdAt.toDouble())
    }

    // ---- Clocks ----

    override fun elapsedRealtime(): Double = SystemClock.elapsedRealtime().toDouble()

    override fun getBootCount(): Double =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0).toDouble()

    // ---- World clock ----

    override fun getWorldClocks(promise: Promise) = guard(promise) {
        promise.resolve(Arguments.createArray().apply { clocks.getAll().forEach { pushMap(clockMap(it)) } })
    }

    override fun addWorldClock(cityName: String, zoneId: String, promise: Promise) = guard(promise) {
        promise.resolve(clockMap(clocks.add(cityName, zoneId)))
    }

    override fun removeWorldClock(clockId: Double, promise: Promise) = guard(promise) {
        clocks.remove(clockId.toLong())
        promise.resolve(null)
    }

    override fun getZoneOffsets(zoneIds: ReadableArray, atMillis: Double, promise: Promise) = guard(promise) {
        val instant = Instant.ofEpochMilli(atMillis.toLong())
        val result = Arguments.createArray()
        for (i in 0 until zoneIds.size()) {
            val id = zoneIds.getString(i) ?: continue
            // "local" is the device's current zone, read fresh so zone changes apply immediately.
            val zone = if (id == LOCAL_ZONE) ZoneId.systemDefault() else runCatching { ZoneId.of(id) }.getOrNull() ?: continue
            val rules = zone.rules
            result.pushMap(
                Arguments.createMap().apply {
                    putString("zoneId", id)
                    putDouble("offsetMillis", rules.getOffset(instant).totalSeconds * 1000.0)
                    putDouble("nextTransitionAt", (rules.nextTransition(instant)?.toEpochSecond()?.times(1000) ?: 0L).toDouble())
                },
            )
        }
        promise.resolve(result)
    }

    private fun clockMap(c: WorldClock) = Arguments.createMap().apply {
        putDouble("id", c.id.toDouble())
        putString("cityName", c.cityName)
        putString("zoneId", c.zoneId)
        putInt("displayOrder", c.displayOrder)
    }

    // ---- Settings and state ----

    override fun getSettings(promise: Promise) = guard(promise) { promise.resolve(settingsMap(SettingsStore.load(context))) }

    override fun saveSettings(settings: ReadableMap, promise: Promise) = guard(promise) {
        val previousLanguage = SettingsStore.load(context).language
        val saved = SettingsStore.save(
            context,
            WakeSettings(
                theme = settings.getString("theme").orEmpty(),
                timeFormat = settings.getString("timeFormat").orEmpty(),
                language = settings.getString("language").orEmpty(),
                defaultSoundUri = settings.getString("defaultSoundUri").orEmpty(),
                defaultSoundName = settings.getString("defaultSoundName").orEmpty(),
                defaultSnoozeMinutes = settings.getDouble("defaultSnoozeMinutes").toInt(),
                defaultVibration = settings.getBoolean("defaultVibration"),
                defaultGradualVolume = settings.getBoolean("defaultGradualVolume"),
                defaultChallengeEnabled = settings.getBoolean("defaultChallengeEnabled"),
                defaultChallengeDifficulty = settings.getString("defaultChallengeDifficulty").orEmpty(),
                defaultChallengeQuestionCount = settings.getDouble("defaultChallengeQuestionCount").toInt(),
                weatherEnabled = settings.getBoolean("weatherEnabled"),
                weatherCity = settings.getString("weatherCity").orEmpty(),
                weatherLatitude = settings.getDouble("weatherLatitude"),
                weatherLongitude = settings.getDouble("weatherLongitude"),
                temperatureUnit = settings.getString("temperatureUnit").orEmpty(),
            ),
        )
        onWeatherSettingsSaved(saved)
        // Channel names are localised; refresh them when the language changes.
        if (saved.language != previousLanguage) WakeNotifications.ensureChannels(context)
        EngineEvents.emit(EngineEvents.SETTINGS_CHANGED)
        promise.resolve(settingsMap(saved))
    }

    private fun settingsMap(s: WakeSettings) = Arguments.createMap().apply {
        putString("theme", s.theme)
        putString("timeFormat", s.timeFormat)
        putString("language", s.language)
        putString("defaultSoundUri", s.defaultSoundUri)
        putString("defaultSoundName", s.defaultSoundName)
        putInt("defaultSnoozeMinutes", s.defaultSnoozeMinutes)
        putBoolean("defaultVibration", s.defaultVibration)
        putBoolean("defaultGradualVolume", s.defaultGradualVolume)
        putBoolean("defaultChallengeEnabled", s.defaultChallengeEnabled)
        putString("defaultChallengeDifficulty", s.defaultChallengeDifficulty)
        putInt("defaultChallengeQuestionCount", s.defaultChallengeQuestionCount)
        putBoolean("weatherEnabled", s.weatherEnabled)
        putString("weatherCity", s.weatherCity)
        putDouble("weatherLatitude", s.weatherLatitude)
        putDouble("weatherLongitude", s.weatherLongitude)
        putString("temperatureUnit", s.temperatureUnit)
    }

    // ---- Weather (opt-in) ----

    /** Disabling weather deletes the cached reading; enabling or changing city fetches in the background. */
    private fun onWeatherSettingsSaved(s: WakeSettings) {
        if (!s.weatherEnabled) {
            WeatherRepository.clear(context)
            ClockWidgetProvider.updateAll(context)
        } else if (WeatherRepository.cached(context) == null) {
            Thread {
                WeatherRepository.refresh(context)
                ClockWidgetProvider.updateAll(context)
            }.start()
        }
    }

    override fun getWeather(promise: Promise) = guard(promise) {
        promise.resolve(WeatherRepository.cached(context)?.let(::weatherMap))
    }

    override fun refreshWeather(promise: Promise) {
        Thread {
            val snapshot = WeatherRepository.refresh(context)
            ClockWidgetProvider.updateAll(context)
            promise.resolve(snapshot?.let(::weatherMap))
        }.start()
    }

    private fun weatherMap(w: WeatherSnapshot) = Arguments.createMap().apply {
        putString("city", w.city)
        putDouble("temperatureCelsius", w.temperatureCelsius)
        putString("condition", w.condition.name)
        putBoolean("isDay", w.isDay)
        putDouble("fetchedAt", w.fetchedAt.toDouble())
    }

    private fun checkKey(key: String) = require(key.matches(KEY_PATTERN)) { "Invalid key" }

    override fun getValue(key: String, promise: Promise) = guard(promise) {
        checkKey(key)
        promise.resolve(AppStorage.state(context).getString("js.$key", null))
    }

    override fun setValue(key: String, value: String, promise: Promise) = guard(promise) {
        checkKey(key)
        require(value.length <= 64_000) { "Value too large" }
        AppStorage.state(context).edit().putString("js.$key", value).apply()
        promise.resolve(null)
    }

    override fun removeValue(key: String, promise: Promise) = guard(promise) {
        checkKey(key)
        AppStorage.state(context).edit().remove("js.$key").apply()
        promise.resolve(null)
    }

    companion object {
        const val NAME = "RcbWake"
        private const val LOCAL_ZONE = "local"
        private const val REQUEST_PICK_SOUND = 4711
        private val KEY_PATTERN = Regex("[a-zA-Z0-9_.-]{1,64}")
    }
}
