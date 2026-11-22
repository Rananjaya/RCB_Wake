package com.rcb.wake.storage

import android.content.Context
import android.text.format.DateFormat
import com.rcb.wake.alarm.AlarmLimits
import com.rcb.wake.alarm.ChallengeDifficulty
import com.rcb.wake.audio.AlarmSounds
import java.util.Locale

data class WakeSettings(
    val theme: String,
    /** `default` (theme's own) or one of [SettingsStore.ACCENTS]. */
    val accentColor: String,
    val timeFormat: String,
    val language: String,
    val defaultSoundUri: String,
    val defaultSoundName: String,
    val defaultSnoozeMinutes: Int,
    val defaultVibration: Boolean,
    val defaultGradualVolume: Boolean,
    val defaultChallengeEnabled: Boolean,
    val defaultChallengeDifficulty: String,
    val defaultChallengeQuestionCount: Int,
    /** Opt-in: when false the app makes no network requests at all. */
    val weatherEnabled: Boolean,
    val weatherCity: String,
    /** Use the device's coarse location instead of [weatherCity] when permission is granted. */
    val weatherUseLocation: Boolean,
    val weatherLatitude: Double,
    val weatherLongitude: Double,
    /** `c` or `f` */
    val temperatureUnit: String,
    val sleepReminderEnabled: Boolean,
    val sleepGoalMinutes: Int,
    val sleepReminderLeadMinutes: Int,
    /** Widget text: `white`, `material` (Material You wallpaper colours, Android 12+) or `accent`. */
    val widgetTextColor: String = "white",
    /** Show the city / village name next to the widget temperature. */
    val widgetShowPlace: Boolean = true,
    /** Show connected earphones (name, Bluetooth battery) on the My Day widget. */
    val widgetShowHeadphones: Boolean = false,
    /** How often the widget weather is refreshed; one of [SettingsStore.WEATHER_REFRESH_MINUTES]. */
    val weatherRefreshMinutes: Int = 30,
    /** `openmeteo` (default, no key) or `openweathermap` (user's own API key). */
    val weatherProvider: String = "openmeteo",
    /** User-supplied OpenWeatherMap key; stays on the device and is only sent to OpenWeatherMap. */
    val openWeatherMapApiKey: String = "",
    /** Device 12/24-hour preference, captured when settings are loaded. */
    val systemIs24Hour: Boolean = false,
) {
    /** `system` follows the device setting; `12` or `24` override it. */
    val is24Hour get() = timeFormat == "24" || (timeFormat == "system" && systemIs24Hour)
}

/** User preferences. Stored natively because the ringing screen needs them without JavaScript. */
object SettingsStore {
    const val THEME_MIDNIGHT = "midnight"
    const val THEME_PINK = "pink"
    val THEMES = setOf(THEME_MIDNIGHT, THEME_PINK)
    val WIDGET_TEXT_COLORS = setOf("white", "material", "accent")
    val WEATHER_REFRESH_MINUTES = setOf(15, 30, 60, 180)
    const val PROVIDER_OPEN_METEO = "openmeteo"
    const val PROVIDER_OPEN_WEATHER_MAP = "openweathermap"
    val WEATHER_PROVIDERS = setOf(PROVIDER_OPEN_METEO, PROVIDER_OPEN_WEATHER_MAP)
    private val API_KEY = Regex("^[A-Za-z0-9]{16,64}$")
    val LANGUAGES = setOf("en", "si", "ta", "de", "ru", "es", "fr", "pt", "hi", "zh", "ja")

    /** Ringing-screen colours per accent: background, primary, surface for dark and light. Mirrors src/theme/themes.ts. */
    data class AccentColors(val darkBg: String, val darkPrimary: String, val darkSurface: String, val lightBg: String, val lightPrimary: String, val lightSurface: String)

    val ACCENTS = mapOf(
        "purple" to AccentColors("#100D1C", "#A855F7", "#28213B", "#F8F3FF", "#8E44D6", "#EBDDF9"),
        "blue" to AccentColors("#0B1220", "#60A5FA", "#1C2945", "#F3F7FF", "#2F6FD6", "#E2EBFB"),
        "teal" to AccentColors("#081614", "#2DD4BF", "#18332F", "#F1FAF8", "#0F8F80", "#D9F0EC"),
        "green" to AccentColors("#0A150E", "#4ADE80", "#1A3023", "#F3FAF4", "#2E8A4E", "#DDF0E1"),
        "orange" to AccentColors("#170F0A", "#FB923C", "#33241A", "#FFF6EF", "#C9601F", "#FBE4D3"),
        "rose" to AccentColors("#170A10", "#FB7185", "#351A26", "#FFF4F2", "#C2577F", "#FBE3EA"),
    )
    fun load(context: Context): WakeSettings {
        val p = AppStorage.settings(context)
        val systemLanguage = Locale.getDefault().language.takeIf { it in LANGUAGES } ?: "en"
        return WakeSettings(
            theme = p.getString("theme", THEME_MIDNIGHT)!!,
            accentColor = p.getString("accentColor", "default")!!,
            timeFormat = p.getString("timeFormat", "system")!!,
            language = p.getString("language", systemLanguage)!!,
            defaultSoundUri = p.getString("defaultSoundUri", AlarmSounds.DEFAULT_URI)!!,
            defaultSoundName = p.getString("defaultSoundName", AlarmSounds.DEFAULT_NAME)!!,
            defaultSnoozeMinutes = p.getInt("defaultSnoozeMinutes", 10),
            defaultVibration = p.getBoolean("defaultVibration", true),
            defaultGradualVolume = p.getBoolean("defaultGradualVolume", true),
            defaultChallengeEnabled = p.getBoolean("defaultChallengeEnabled", false),
            defaultChallengeDifficulty = p.getString("defaultChallengeDifficulty", ChallengeDifficulty.EASY)!!,
            defaultChallengeQuestionCount = p.getInt("defaultChallengeQuestionCount", 3),
            weatherEnabled = p.getBoolean("weatherEnabled", false),
            weatherCity = p.getString("weatherCity", "")!!,
            weatherUseLocation = p.getBoolean("weatherUseLocation", false),
            weatherLatitude = p.getFloat("weatherLatitude", 0f).toDouble(),
            weatherLongitude = p.getFloat("weatherLongitude", 0f).toDouble(),
            temperatureUnit = p.getString("temperatureUnit", "c")!!,
            sleepReminderEnabled = p.getBoolean("sleepReminderEnabled", false),
            sleepGoalMinutes = p.getInt("sleepGoalMinutes", 8 * 60),
            sleepReminderLeadMinutes = p.getInt("sleepReminderLeadMinutes", 30),
            widgetTextColor = p.getString("widgetTextColor", "white")!!,
            weatherRefreshMinutes = p.getInt("weatherRefreshMinutes", 30),
            widgetShowPlace = p.getBoolean("widgetShowPlace", true),
            widgetShowHeadphones = p.getBoolean("widgetShowHeadphones", false),
            weatherProvider = p.getString("weatherProvider", PROVIDER_OPEN_METEO)!!,
            openWeatherMapApiKey = p.getString("openWeatherMapApiKey", "")!!,
            systemIs24Hour = DateFormat.is24HourFormat(context),
        )
    }

    fun save(context: Context, s: WakeSettings): WakeSettings {
        require(s.theme in THEMES) { "unknown theme" }
        require(s.accentColor == "default" || s.accentColor in ACCENTS) { "unknown accent" }
        require(s.timeFormat in setOf("system", "12", "24")) { "unknown time format" }
        require(s.language in LANGUAGES) { "unknown language" }
        require(s.defaultSnoozeMinutes in AlarmLimits.MIN_SNOOZE_MINUTES..AlarmLimits.MAX_SNOOZE_MINUTES) { "snooze" }
        require(s.defaultChallengeDifficulty in ChallengeDifficulty.ALL) { "difficulty" }
        require(s.defaultChallengeQuestionCount in AlarmLimits.MIN_QUESTIONS..AlarmLimits.MAX_QUESTIONS) { "questions" }
        require(AlarmSounds.isSupportedUri(s.defaultSoundUri)) { "sound" }
        require(s.temperatureUnit == "c" || s.temperatureUnit == "f") { "temperature unit" }
        require(s.weatherLatitude in -90.0..90.0 && s.weatherLongitude in -180.0..180.0) { "coordinates" }
        require(!s.weatherEnabled || s.weatherCity.isNotBlank() || s.weatherUseLocation) { "weather city required" }
        require(s.sleepGoalMinutes in 240..720) { "sleep goal 4-12 h" }
        require(s.sleepReminderLeadMinutes in 0..120) { "reminder lead 0-120 min" }
        require(s.widgetTextColor in WIDGET_TEXT_COLORS) { "widget text colour" }
        require(s.weatherRefreshMinutes in WEATHER_REFRESH_MINUTES) { "weather refresh interval" }
        require(s.weatherProvider in WEATHER_PROVIDERS) { "weather provider" }
        require(s.openWeatherMapApiKey.isEmpty() || API_KEY.matches(s.openWeatherMapApiKey)) { "API key" }
        require(!s.weatherEnabled || s.weatherProvider != PROVIDER_OPEN_WEATHER_MAP || s.openWeatherMapApiKey.isNotEmpty()) { "API key required" }
        AppStorage.settings(context).edit()
            .putString("theme", s.theme)
            .putString("accentColor", s.accentColor)
            .putString("timeFormat", s.timeFormat)
            .putString("language", s.language)
            .putString("defaultSoundUri", s.defaultSoundUri)
            .putString("defaultSoundName", s.defaultSoundName.take(120))
            .putInt("defaultSnoozeMinutes", s.defaultSnoozeMinutes)
            .putBoolean("defaultVibration", s.defaultVibration)
            .putBoolean("defaultGradualVolume", s.defaultGradualVolume)
            .putBoolean("defaultChallengeEnabled", s.defaultChallengeEnabled)
            .putString("defaultChallengeDifficulty", s.defaultChallengeDifficulty)
            .putInt("defaultChallengeQuestionCount", s.defaultChallengeQuestionCount)
            .putBoolean("weatherEnabled", s.weatherEnabled)
            .putString("weatherCity", s.weatherCity.take(80))
            .putBoolean("weatherUseLocation", s.weatherUseLocation)
            .putFloat("weatherLatitude", s.weatherLatitude.toFloat())
            .putFloat("weatherLongitude", s.weatherLongitude.toFloat())
            .putString("temperatureUnit", s.temperatureUnit)
            .putBoolean("sleepReminderEnabled", s.sleepReminderEnabled)
            .putInt("sleepGoalMinutes", s.sleepGoalMinutes)
            .putInt("sleepReminderLeadMinutes", s.sleepReminderLeadMinutes)
            .putString("widgetTextColor", s.widgetTextColor)
            .putInt("weatherRefreshMinutes", s.weatherRefreshMinutes)
            .putBoolean("widgetShowPlace", s.widgetShowPlace)
            .putBoolean("widgetShowHeadphones", s.widgetShowHeadphones)
            .putString("weatherProvider", s.weatherProvider)
            .putString("openWeatherMapApiKey", s.openWeatherMapApiKey.trim())
            .commit()
        return load(context)
    }
}
