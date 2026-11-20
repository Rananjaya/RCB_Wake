package com.rcb.wake.storage

import android.content.Context
import android.text.format.DateFormat
import com.rcb.wake.alarm.AlarmLimits
import com.rcb.wake.alarm.ChallengeDifficulty
import com.rcb.wake.audio.AlarmSounds
import java.util.Locale

data class WakeSettings(
    val theme: String,
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
    val weatherLatitude: Double,
    val weatherLongitude: Double,
    /** `c` or `f` */
    val temperatureUnit: String,
) {
    val is24Hour get() = timeFormat == "24"
}

/** User preferences. Stored natively because the ringing screen needs them without JavaScript. */
object SettingsStore {
    const val THEME_MIDNIGHT = "midnight"
    const val THEME_PINK = "pink"
    val THEMES = setOf(THEME_MIDNIGHT, THEME_PINK)
    val LANGUAGES = setOf("en", "si", "ta")

    fun load(context: Context): WakeSettings {
        val p = AppStorage.settings(context)
        val systemLanguage = Locale.getDefault().language.takeIf { it in LANGUAGES } ?: "en"
        val systemFormat = if (DateFormat.is24HourFormat(context)) "24" else "12"
        return WakeSettings(
            theme = p.getString("theme", THEME_MIDNIGHT)!!,
            timeFormat = p.getString("timeFormat", systemFormat)!!,
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
            weatherLatitude = p.getFloat("weatherLatitude", 0f).toDouble(),
            weatherLongitude = p.getFloat("weatherLongitude", 0f).toDouble(),
            temperatureUnit = p.getString("temperatureUnit", "c")!!,
        )
    }

    fun save(context: Context, s: WakeSettings): WakeSettings {
        require(s.theme in THEMES) { "unknown theme" }
        require(s.timeFormat == "12" || s.timeFormat == "24") { "unknown time format" }
        require(s.language in LANGUAGES) { "unknown language" }
        require(s.defaultSnoozeMinutes in AlarmLimits.MIN_SNOOZE_MINUTES..AlarmLimits.MAX_SNOOZE_MINUTES) { "snooze" }
        require(s.defaultChallengeDifficulty in ChallengeDifficulty.ALL) { "difficulty" }
        require(s.defaultChallengeQuestionCount in AlarmLimits.MIN_QUESTIONS..AlarmLimits.MAX_QUESTIONS) { "questions" }
        require(AlarmSounds.isSupportedUri(s.defaultSoundUri)) { "sound" }
        require(s.temperatureUnit == "c" || s.temperatureUnit == "f") { "temperature unit" }
        require(s.weatherLatitude in -90.0..90.0 && s.weatherLongitude in -180.0..180.0) { "coordinates" }
        require(!s.weatherEnabled || s.weatherCity.isNotBlank()) { "weather city required" }
        AppStorage.settings(context).edit()
            .putString("theme", s.theme)
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
            .putFloat("weatherLatitude", s.weatherLatitude.toFloat())
            .putFloat("weatherLongitude", s.weatherLongitude.toFloat())
            .putString("temperatureUnit", s.temperatureUnit)
            .commit()
        return load(context)
    }
}
