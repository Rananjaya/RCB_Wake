package com.rcb.wake.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerMathTest {
    private fun timer(status: TimerStatus, remaining: Long = 60_000, endElapsed: Long? = null, endWall: Long? = null) =
        WakeTimer(1, "", 60_000, remaining, endElapsed, endWall, status, 0)

    @Test fun `running timer counts down on the monotonic clock`() {
        assertEquals(15_000, TimerMath.remaining(timer(TimerStatus.RUNNING, endElapsed = 100_000), 85_000))
    }

    @Test fun `running timer past its end reports zero`() {
        assertEquals(0, TimerMath.remaining(timer(TimerStatus.RUNNING, endElapsed = 100_000), 120_000))
    }

    @Test fun `paused timer keeps its remaining time`() {
        assertEquals(42_000, TimerMath.remaining(timer(TimerStatus.PAUSED, remaining = 42_000), 999_999))
    }

    @Test fun `finished timer is zero`() {
        assertEquals(0, TimerMath.remaining(timer(TimerStatus.FINISHED), 0))
    }

    @Test fun `after reboot remaining time comes from wall clock`() {
        val t = timer(TimerStatus.RUNNING, endElapsed = 9_999_999, endWall = 1_000_000)
        assertEquals(30_000, TimerMath.remainingAfterReconcile(t, nowElapsed = 5, nowWall = 970_000, rebooted = true))
    }

    @Test fun `without reboot the monotonic clock is used even if wall clock jumped`() {
        val t = timer(TimerStatus.RUNNING, endElapsed = 100_000, endWall = 1_000_000)
        assertEquals(40_000, TimerMath.remainingAfterReconcile(t, nowElapsed = 60_000, nowWall = 5_000_000, rebooted = false))
    }

    @Test fun `durations are validated`() {
        assertEquals(1000, TimerMath.validateDuration(1000))
        assertEquals(TimerMath.MAX_DURATION_MILLIS, TimerMath.validateDuration(TimerMath.MAX_DURATION_MILLIS))
    }

    @Test(expected = IllegalArgumentException::class) fun `zero duration rejected`() { TimerMath.validateDuration(0) }
    @Test(expected = IllegalArgumentException::class) fun `over 99 hours rejected`() { TimerMath.validateDuration(TimerMath.MAX_DURATION_MILLIS + 1000) }
}
