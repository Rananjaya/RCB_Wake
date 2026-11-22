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

    private fun content(event: CalendarEvent? = null, alarmAt: Long? = null) =
        WidgetContent(false, "system", null, alarmAt, null, "", "", event, null)

    @Test fun `event or alarm within ten minutes is soon`() {
        val now = 1_000_000_000L
        val min = 60_000L
        assertEquals(null, GlanceWidgetProvider.soonUntil(content(), now))
        val event = CalendarEvent("Standup", now + 9 * min, now + 30 * min, allDay = false)
        assertEquals(now + 30 * min, GlanceWidgetProvider.soonUntil(content(event = event), now))
        val later = CalendarEvent("Lunch", now + 11 * min, now + 60 * min, allDay = false)
        assertEquals(null, GlanceWidgetProvider.soonUntil(content(event = later), now))
        val allDay = CalendarEvent("Holiday", now - 60 * min, now + 600 * min, allDay = true)
        assertEquals(null, GlanceWidgetProvider.soonUntil(content(event = allDay), now))
        assertEquals(now + 5 * min, GlanceWidgetProvider.soonUntil(content(alarmAt = now + 5 * min), now))
        assertEquals(null, GlanceWidgetProvider.soonUntil(content(alarmAt = now + 20 * min), now))
    }
}
