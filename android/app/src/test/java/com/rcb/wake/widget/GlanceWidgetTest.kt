package com.rcb.wake.widget

import com.rcb.wake.widget.GlanceWidgetProvider.Greeting
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class GlanceWidgetTest {
    @Test fun `greeting follows the time of day`() {
        assertEquals(Greeting.NIGHT, GlanceWidgetProvider.greetingFor(4))
        assertEquals(Greeting.MORNING, GlanceWidgetProvider.greetingFor(5))
        assertEquals(Greeting.MORNING, GlanceWidgetProvider.greetingFor(11))
        assertEquals(Greeting.AFTERNOON, GlanceWidgetProvider.greetingFor(12))
        assertEquals(Greeting.EVENING, GlanceWidgetProvider.greetingFor(17))
        assertEquals(Greeting.NIGHT, GlanceWidgetProvider.greetingFor(21))
    }

    @Test fun `next greeting change`() {
        assertEquals(LocalDateTime.of(2026, 11, 22, 12, 0), GlanceWidgetProvider.nextBoundary(LocalDateTime.of(2026, 11, 22, 9, 30)))
        assertEquals(LocalDateTime.of(2026, 11, 22, 17, 0), GlanceWidgetProvider.nextBoundary(LocalDateTime.of(2026, 11, 22, 12, 0)))
        assertEquals(LocalDateTime.of(2026, 11, 23, 5, 0), GlanceWidgetProvider.nextBoundary(LocalDateTime.of(2026, 11, 22, 22, 15)))
    }
}
