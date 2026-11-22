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

    @Test fun `clock size fits the widget and stays within limits`() {
        // Clock plus rows below must fit the height: 1.2 em per line.
        val large = ClockWidgetProvider.clockSizeDp(Layout.LARGE, 250f, 200f)
        assert(large * 1.2f + 90f + 32f <= 200f) { "large $large" }
        assertEquals(96f, ClockWidgetProvider.clockSizeDp(Layout.LARGE, 400f, 400f))
        assertEquals(28f, ClockWidgetProvider.clockSizeDp(Layout.LARGE, 100f, 100f))
        val wide = ClockWidgetProvider.clockSizeDp(Layout.WIDE, 300f, 140f)
        assert(wide * 2.7f <= 300f - 170f) { "wide $wide" }
    }
}
