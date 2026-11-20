package com.rcb.wake.sleep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SleepMathTest {
    private val hour = 3_600_000L

    @Test fun `reminder is goal plus lead before the alarm`() {
        val alarm = 30 * hour
        assertEquals(alarm - 8 * hour - 30 * 60_000L, SleepMath.reminderAt(alarm, 8 * 60, 30, now = 0))
    }

    @Test fun `no reminder when bedtime already passed`() {
        val alarm = 6 * hour
        assertNull(SleepMath.reminderAt(alarm, 8 * 60, 30, now = 0))
    }

    @Test fun `no reminder without an upcoming alarm`() {
        assertNull(SleepMath.reminderAt(null, 480, 30, now = 0))
        assertNull(SleepMath.reminderAt(10, 480, 30, now = 20))
    }

    @Test fun `bedtime is the sleep goal before the alarm`() {
        assertEquals(22 * hour, SleepMath.bedtime(30 * hour, 8 * 60))
    }
}
