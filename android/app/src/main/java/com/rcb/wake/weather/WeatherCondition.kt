package com.rcb.wake.weather

/** Coarse weather categories mapped from WMO weather interpretation codes (as used by Open-Meteo). */
enum class WeatherCondition {
    CLEAR, MOSTLY_CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, HEAVY_RAIN, SHOWERS, SLEET, SNOW, SNOW_SHOWERS, THUNDERSTORM,
}

object WeatherCodes {
    fun condition(wmoCode: Int): WeatherCondition = when (wmoCode) {
        0 -> WeatherCondition.CLEAR
        1 -> WeatherCondition.MOSTLY_CLEAR
        2 -> WeatherCondition.PARTLY_CLOUDY
        3 -> WeatherCondition.CLOUDY
        45, 48 -> WeatherCondition.FOG
        56, 57, 66, 67 -> WeatherCondition.SLEET  // freezing drizzle / freezing rain
        in 51..55 -> WeatherCondition.DRIZZLE
        65 -> WeatherCondition.HEAVY_RAIN
        in 61..64 -> WeatherCondition.RAIN
        82 -> WeatherCondition.HEAVY_RAIN
        80, 81 -> WeatherCondition.SHOWERS
        in 71..77 -> WeatherCondition.SNOW
        85, 86 -> WeatherCondition.SNOW_SHOWERS
        in 95..99 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.CLOUDY
    }

    /** OpenWeatherMap condition ids (https://openweathermap.org/weather-conditions). */
    fun openWeatherCondition(id: Int): WeatherCondition = when (id) {
        in 200..299 -> WeatherCondition.THUNDERSTORM
        in 300..399 -> WeatherCondition.DRIZZLE
        511, in 611..616 -> WeatherCondition.SLEET
        in 520..531 -> WeatherCondition.SHOWERS
        in 502..504 -> WeatherCondition.HEAVY_RAIN
        in 500..510 -> WeatherCondition.RAIN
        in 620..622 -> WeatherCondition.SNOW_SHOWERS
        in 600..699 -> WeatherCondition.SNOW
        in 700..799 -> WeatherCondition.FOG
        800 -> WeatherCondition.CLEAR
        801 -> WeatherCondition.MOSTLY_CLEAR
        802 -> WeatherCondition.PARTLY_CLOUDY
        else -> WeatherCondition.CLOUDY
    }

    /** MET Norway symbol codes (e.g. `partlycloudy_day`, `heavyrainshowers_night`, `lightsleet`). */
    fun metNorwayCondition(symbol: String): WeatherCondition {
        val s = symbol.substringBefore('_')
        return when {
            "thunder" in s -> WeatherCondition.THUNDERSTORM
            "sleet" in s -> WeatherCondition.SLEET
            "snowshowers" in s -> WeatherCondition.SNOW_SHOWERS
            "snow" in s -> WeatherCondition.SNOW
            s.startsWith("heavyrain") -> WeatherCondition.HEAVY_RAIN
            "rainshowers" in s -> WeatherCondition.SHOWERS
            s == "lightrain" -> WeatherCondition.DRIZZLE
            "rain" in s -> WeatherCondition.RAIN
            s == "fog" -> WeatherCondition.FOG
            s == "clearsky" -> WeatherCondition.CLEAR
            s == "fair" -> WeatherCondition.MOSTLY_CLEAR
            s == "partlycloudy" -> WeatherCondition.PARTLY_CLOUDY
            else -> WeatherCondition.CLOUDY
        }
    }

    fun celsiusToFahrenheit(c: Double): Double = c * 9.0 / 5.0 + 32.0

    fun formatTemperature(celsius: Double, unit: String): String {
        val value = if (unit == "f") celsiusToFahrenheit(celsius) else celsius
        return "${Math.round(value)}°${if (unit == "f") "F" else "C"}"
    }
}
