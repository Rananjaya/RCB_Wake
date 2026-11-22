package com.rcb.wake.weather

/** Coarse weather categories mapped from WMO weather interpretation codes (as used by Open-Meteo). */
enum class WeatherCondition { CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SHOWERS, SNOW, SNOW_SHOWERS, THUNDERSTORM }

object WeatherCodes {
    fun condition(wmoCode: Int): WeatherCondition = when (wmoCode) {
        0 -> WeatherCondition.CLEAR
        1, 2 -> WeatherCondition.PARTLY_CLOUDY
        3 -> WeatherCondition.CLOUDY
        45, 48 -> WeatherCondition.FOG
        in 51..57 -> WeatherCondition.DRIZZLE
        in 61..67 -> WeatherCondition.RAIN
        in 80..82 -> WeatherCondition.SHOWERS
        in 71..77 -> WeatherCondition.SNOW
        85, 86 -> WeatherCondition.SNOW_SHOWERS
        in 95..99 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.CLOUDY
    }

    /** OpenWeatherMap condition ids (https://openweathermap.org/weather-conditions). */
    fun openWeatherCondition(id: Int): WeatherCondition = when (id) {
        in 200..299 -> WeatherCondition.THUNDERSTORM
        in 300..399 -> WeatherCondition.DRIZZLE
        in 520..531 -> WeatherCondition.SHOWERS
        in 500..519 -> WeatherCondition.RAIN
        in 620..622 -> WeatherCondition.SNOW_SHOWERS
        in 600..699 -> WeatherCondition.SNOW
        in 700..799 -> WeatherCondition.FOG
        800 -> WeatherCondition.CLEAR
        801, 802 -> WeatherCondition.PARTLY_CLOUDY
        else -> WeatherCondition.CLOUDY
    }

    fun celsiusToFahrenheit(c: Double): Double = c * 9.0 / 5.0 + 32.0

    fun formatTemperature(celsius: Double, unit: String): String {
        val value = if (unit == "f") celsiusToFahrenheit(celsius) else celsius
        return "${Math.round(value)}°${if (unit == "f") "F" else "C"}"
    }
}
