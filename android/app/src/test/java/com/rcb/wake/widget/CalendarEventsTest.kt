package com.rcb.wake.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarEventsTest {
    private val hour = 3_600_000L
    private fun event(title: String, begin: Long, end: Long, allDay: Boolean = false) = CalendarEvent(title, begin, end, allDay)

    @Test fun `ended events are dropped so the next one shows`() {
        val now = 10 * hour
        val events = listOf(event("Done", 8 * hour, 9 * hour), event("Next", 11 * hour, 12 * hour))
        assertEquals("Next", CalendarEvents.selectNext(events, now)?.title)
    }

    @Test fun `ongoing event is shown until it ends`() {
        val events = listOf(event("Meeting", 9 * hour, 11 * hour), event("Lunch", 12 * hour, 13 * hour))
        assertEquals("Meeting", CalendarEvents.selectNext(events, 10 * hour)?.title)
    }

    @Test fun `timed events win over all-day events`() {
        val events = listOf(event("Holiday", 0, 24 * hour, allDay = true), event("Call", 15 * hour, 16 * hour))
        assertEquals("Call", CalendarEvents.selectNext(events, 10 * hour)?.title)
    }

    @Test fun `all-day event shows when nothing timed remains`() {
        val events = listOf(event("Holiday", 0, 24 * hour, allDay = true))
        assertEquals("Holiday", CalendarEvents.selectNext(events, 10 * hour)?.title)
    }

    @Test fun `untitled events and empty lists show nothing`() {
        assertNull(CalendarEvents.selectNext(listOf(event(" ", 11 * hour, 12 * hour)), 10 * hour))
        assertNull(CalendarEvents.selectNext(emptyList(), 10 * hour))
    }
}
