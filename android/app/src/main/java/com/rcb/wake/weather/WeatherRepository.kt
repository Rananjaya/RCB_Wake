package com.rcb.wake.weather

import android.content.Context
import android.util.Log
import com.rcb.wake.storage.AppStorage
import com.rcb.wake.storage.SettingsStore
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class WeatherSnapshot(
    val temperatureCelsius: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val fetchedAt: Long,
    val city: String,
    /** Rounded coordinates that were sent to the provider. */
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    /** Place the provider matched the coordinates to (OpenWeatherMap station/city name), if any. */
    val place: String = "",
)

/**
 * Opt-in current weather from Open-Meteo (https://open-meteo.com, data CC BY 4.0, no key) or, if the
 * user chooses it, OpenWeatherMap with their own API key. Only rounded coordinates (and that key)
 * are sent. When weather is disabled in Settings this class performs no network access.
 */
object WeatherRepository {
    private const val TAG = "RcbWakeWeather"
    private const val KEY = "weather_snapshot"
    /** Slack so a refresh alarm delivered slightly early still counts the reading as stale. */
    private const val EARLY_SLACK_MILLIS = 2 * 60_000L
    /** Snapshot key used when weather comes from the device location. */
    const val LOCATION_KEY = "@location"

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
                latitude = o.optDouble("lat", 0.0),
                longitude = o.optDouble("lon", 0.0),
                place = o.optString("place"),
            ) to o.optString("p", SettingsStore.PROVIDER_OPEN_METEO)
        }.getOrNull()
            // A reading from another place or provider is discarded so the widget never shows it.
            ?.takeIf { (snapshot, provider) -> snapshot.city == sourceKey(context) && provider == settings.weatherProvider }
            ?.first
    }

    fun isStale(context: Context): Boolean {
        val snapshot = cached(context) ?: return true
        val maxAge = SettingsStore.load(context).weatherRefreshMinutes * 60_000L - EARLY_SLACK_MILLIS
        return System.currentTimeMillis() - snapshot.fetchedAt > maxAge
    }

    fun clear(context: Context) = AppStorage.state(context).edit().remove(KEY).apply()

    /** Where weather is read for: the device location when enabled and known, else the chosen city. */
    private fun source(context: Context): Pair<String, Coordinates>? {
        val settings = SettingsStore.load(context)
        if (settings.weatherUseLocation && DeviceLocation.hasPermission(context)) {
            DeviceLocation.stored(context)?.let { return LOCATION_KEY to it }
        }
        if (settings.weatherCity.isBlank()) return null
        return settings.weatherCity to Coordinates(settings.weatherLatitude, settings.weatherLongitude)
    }

    private fun sourceKey(context: Context) = source(context)?.first

    /** Blocking network call; never call on the main thread. Returns null when disabled or on failure. */
    fun refresh(context: Context): WeatherSnapshot? {
        val settings = SettingsStore.load(context)
        if (!settings.weatherEnabled) return null
        val (key, coordinates) = source(context) ?: return null
        return try {
            val sent = Coordinates(round2(coordinates.latitude), round2(coordinates.longitude))
            val snapshot = when (settings.weatherProvider) {
                SettingsStore.PROVIDER_OPEN_WEATHER_MAP -> fetchOpenWeatherMap(sent, settings.openWeatherMapApiKey, key)
                else -> fetchOpenMeteo(sent, key)
            } ?: return null
            save(context, snapshot, settings.weatherProvider)
            snapshot
        } catch (e: Exception) {
            // Never log the URL: for OpenWeatherMap it contains the user's API key.
            Log.w(TAG, "Weather refresh failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private fun round2(v: Double) = Math.round(v * 100.0) / 100.0

    private fun fetchOpenMeteo(c: Coordinates, key: String): WeatherSnapshot? {
        val body = get(
            String.format(
                Locale.ROOT,
                "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=temperature_2m,weather_code,is_day",
                c.latitude,
                c.longitude,
            ),
        ) ?: return null
        val current = JSONObject(body).getJSONObject("current")
        return WeatherSnapshot(
            temperatureCelsius = current.getDouble("temperature_2m"),
            condition = WeatherCodes.condition(current.getInt("weather_code")),
            isDay = current.optInt("is_day", 1) == 1,
            fetchedAt = System.currentTimeMillis(),
            city = key,
            latitude = c.latitude,
            longitude = c.longitude,
        )
    }

    /** OpenWeatherMap current weather with the user's own key (free tier works). */
    private fun fetchOpenWeatherMap(c: Coordinates, apiKey: String, key: String): WeatherSnapshot? {
        if (apiKey.isBlank()) return null
        val body = get(
            String.format(
                Locale.ROOT,
                "https://api.openweathermap.org/data/2.5/weather?lat=%.2f&lon=%.2f&units=metric&appid=%s",
                c.latitude,
                c.longitude,
                URLEncoder.encode(apiKey, "UTF-8"),
            ),
        ) ?: return null
        val o = JSONObject(body)
        val weather = o.getJSONArray("weather").getJSONObject(0)
        return WeatherSnapshot(
            temperatureCelsius = o.getJSONObject("main").getDouble("temp"),
            condition = WeatherCodes.openWeatherCondition(weather.getInt("id")),
            isDay = !weather.optString("icon").endsWith("n"),
            fetchedAt = System.currentTimeMillis(),
            city = key,
            latitude = c.latitude,
            longitude = c.longitude,
            place = o.optString("name"),
        )
    }

    private fun get(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode != 200) {
                Log.w(TAG, "Weather HTTP ${connection.responseCode}")
                return null
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun save(context: Context, s: WeatherSnapshot, provider: String) {
        val o = JSONObject()
            .put("t", s.temperatureCelsius)
            .put("c", s.condition.name)
            .put("d", s.isDay)
            .put("at", s.fetchedAt)
            .put("city", s.city)
            .put("p", provider)
            .put("lat", s.latitude)
            .put("lon", s.longitude)
            .put("place", s.place)
        AppStorage.state(context).edit().putString(KEY, o.toString()).apply()
    }
}
