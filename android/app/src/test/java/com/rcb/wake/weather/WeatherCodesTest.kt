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
        assertEquals(WeatherCondition.RAIN, WeatherCodes.condition(63))
        assertEquals(WeatherCondition.RAIN, WeatherCodes.condition(81))
        assertEquals(WeatherCondition.SNOW, WeatherCodes.condition(75))
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherCodes.condition(95))
        assertEquals(WeatherCondition.CLOUDY, WeatherCodes.condition(1234))
    }

    @Test fun `temperature formatting`() {
        assertEquals("30°C", WeatherCodes.formatTemperature(29.7, "c"))
        assertEquals("86°F", WeatherCodes.formatTemperature(30.0, "f"))
        assertEquals("-2°C", WeatherCodes.formatTemperature(-2.4, "c"))
    }
}
