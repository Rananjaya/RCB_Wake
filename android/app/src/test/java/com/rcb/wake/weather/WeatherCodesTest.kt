package com.rcb.wake.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodesTest {
    @Test fun `wmo codes map to conditions`() {
        assertEquals(WeatherCondition.CLEAR, WeatherCodes.condition(0))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, WeatherCodes.condition(2))
        assertEquals(WeatherCondition.CLOUDY, WeatherCodes.condition(3))
        assertEquals(WeatherCondition.FOG, WeatherCodes.condition(45))
        assertEquals(WeatherCondition.DRIZZLE, WeatherCodes.condition(53))
        assertEquals(WeatherCondition.MOSTLY_CLEAR, WeatherCodes.condition(1))
        assertEquals(WeatherCondition.HEAVY_RAIN, WeatherCodes.condition(65))
        assertEquals(WeatherCondition.SLEET, WeatherCodes.condition(67))
        assertEquals(WeatherCondition.RAIN, WeatherCodes.condition(63))
        assertEquals(WeatherCondition.SHOWERS, WeatherCodes.condition(81))
        assertEquals(WeatherCondition.SNOW_SHOWERS, WeatherCodes.condition(85))
        assertEquals(WeatherCondition.SNOW, WeatherCodes.condition(75))
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherCodes.condition(95))
        assertEquals(WeatherCondition.CLOUDY, WeatherCodes.condition(1234))
    }

    @Test fun `temperature formatting`() {
        assertEquals("30°C", WeatherCodes.formatTemperature(29.7, "c"))
        assertEquals("86°F", WeatherCodes.formatTemperature(30.0, "f"))
        assertEquals("-2°C", WeatherCodes.formatTemperature(-2.4, "c"))
    }

    @Test
    fun mapsOpenWeatherMapIds() {
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherCodes.openWeatherCondition(211))
        assertEquals(WeatherCondition.DRIZZLE, WeatherCodes.openWeatherCondition(301))
        assertEquals(WeatherCondition.RAIN, WeatherCodes.openWeatherCondition(501))
        assertEquals(WeatherCondition.SHOWERS, WeatherCodes.openWeatherCondition(521))
        assertEquals(WeatherCondition.SNOW, WeatherCodes.openWeatherCondition(601))
        assertEquals(WeatherCondition.SNOW_SHOWERS, WeatherCodes.openWeatherCondition(621))
        assertEquals(WeatherCondition.FOG, WeatherCodes.openWeatherCondition(741))
        assertEquals(WeatherCondition.CLEAR, WeatherCodes.openWeatherCondition(800))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, WeatherCodes.openWeatherCondition(802))
        assertEquals(WeatherCondition.MOSTLY_CLEAR, WeatherCodes.openWeatherCondition(801))
        assertEquals(WeatherCondition.HEAVY_RAIN, WeatherCodes.openWeatherCondition(502))
        assertEquals(WeatherCondition.SLEET, WeatherCodes.openWeatherCondition(611))
        assertEquals(WeatherCondition.CLOUDY, WeatherCodes.openWeatherCondition(804))
    }
}
