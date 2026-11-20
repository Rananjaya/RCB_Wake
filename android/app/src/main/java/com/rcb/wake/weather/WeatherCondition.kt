package com.rcb.wake.weather

/** Coarse weather categories mapped from WMO weather interpretation codes (as used by Open-Meteo). */
enum class WeatherCondition { CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, THUNDERSTORM }

object WeatherCodes {
    fun condition(wmoCode: Int): WeatherCondition = when (wmoCode) {
        0 -> WeatherCondition.CLEAR
        1, 2 -> WeatherCondition.PARTLY_CLOUDY
        3 -> WeatherCondition.CLOUDY
        45, 48 -> WeatherCondition.FOG
        in 51..57 -> WeatherCondition.DRIZZLE
        in 61..67, in 80..82 -> WeatherCondition.RAIN
        in 71..77, 85, 86 -> WeatherCondition.SNOW
        in 95..99 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.CLOUDY
    }

    fun celsiusToFahrenheit(c: Double): Double = c * 9.0 / 5.0 + 32.0

    fun formatTemperature(celsius: Double, unit: String): String {
        val value = if (unit == "f") celsiusToFahrenheit(celsius) else celsius
        return "${Math.round(value)}°${if (unit == "f") "F" else "C"}"
    }
}
