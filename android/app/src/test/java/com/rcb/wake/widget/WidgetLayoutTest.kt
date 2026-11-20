package com.rcb.wake.widget

import com.rcb.wake.widget.ClockWidgetProvider.Layout
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLayoutTest {
    @Test fun `layout follows widget size`() {
        assertEquals(Layout.SMALL, ClockWidgetProvider.layoutFor(110, 110))
        assertEquals(Layout.BAR, ClockWidgetProvider.layoutFor(300, 60))
        assertEquals(Layout.MEDIUM, ClockWidgetProvider.layoutFor(200, 150))
        assertEquals(Layout.WIDE, ClockWidgetProvider.layoutFor(320, 150))
        assertEquals(Layout.LARGE, ClockWidgetProvider.layoutFor(320, 260))
    }
}
