package com.rcb.wake.weather

import android.content.Context
import android.util.Log
import com.rcb.wake.storage.AppStorage
import com.rcb.wake.storage.SettingsStore
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class WeatherSnapshot(
    val temperatureCelsius: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val fetchedAt: Long,
    val city: String,
)

/**
 * Opt-in current weather from Open-Meteo (https://open-meteo.com, data CC BY 4.0). Only the chosen
 * city's rounded coordinates are sent; there is no API key, account or identifier. When weather is
 * disabled in Settings this class performs no network access.
 */
object WeatherRepository {
    private const val TAG = "RcbWakeWeather"
    private const val KEY = "weather_snapshot"
    const val MAX_AGE_MILLIS = 30 * 60_000L

    fun cached(context: Context): WeatherSnapshot? {
        val settings = SettingsStore.load(context)
        if (!settings.weatherEnabled) return null
        val raw = AppStorage.state(context).getString(KEY, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            WeatherSnapshot(
                temperatureCelsius = o.getDouble("t"),
                condition = WeatherCondition.valueOf(o.getString("c")),
                isDay = o.getBoolean("d"),
                fetchedAt = o.getLong("at"),
                city = o.getString("city"),
            )
        }.getOrNull()?.takeIf { it.city == settings.weatherCity }
    }

    fun isStale(context: Context): Boolean {
        val snapshot = cached(context) ?: return true
        return System.currentTimeMillis() - snapshot.fetchedAt > MAX_AGE_MILLIS
    }

    fun clear(context: Context) = AppStorage.state(context).edit().remove(KEY).apply()

    /** Blocking network call; never call on the main thread. Returns null when disabled or on failure. */
    fun refresh(context: Context): WeatherSnapshot? {
        val settings = SettingsStore.load(context)
        if (!settings.weatherEnabled || settings.weatherCity.isBlank()) return null
        val url = URL(
            String.format(
                Locale.ROOT,
                "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=temperature_2m,weather_code,is_day",
                settings.weatherLatitude,
                settings.weatherLongitude,
            ),
        )
        return try {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode != 200) return null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val current = JSONObject(body).getJSONObject("current")
                val snapshot = WeatherSnapshot(
                    temperatureCelsius = current.getDouble("temperature_2m"),
                    condition = WeatherCodes.condition(current.getInt("weather_code")),
                    isDay = current.optInt("is_day", 1) == 1,
                    fetchedAt = System.currentTimeMillis(),
                    city = settings.weatherCity,
                )
                save(context, snapshot)
                snapshot
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Weather refresh failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private fun save(context: Context, s: WeatherSnapshot) {
        val o = JSONObject()
            .put("t", s.temperatureCelsius)
            .put("c", s.condition.name)
            .put("d", s.isDay)
            .put("at", s.fetchedAt)
            .put("city", s.city)
        AppStorage.state(context).edit().putString(KEY, o.toString()).apply()
    }
}
