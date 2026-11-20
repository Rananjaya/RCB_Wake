package com.rcb.wake.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroMathTest {
    private val config = PomodoroConfig(focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15, roundsBeforeLongBreak = 3)

    @Test fun `focus leads to short breaks then a long break after the configured rounds`() {
        var s = PomodoroState(config = config)
        val phases = mutableListOf<PomodoroPhase>()
        repeat(6) {
            s = PomodoroMath.advance(s)
            phases += s.phase
        }
        assertEquals(
            listOf(
                PomodoroPhase.SHORT_BREAK, PomodoroPhase.FOCUS, PomodoroPhase.SHORT_BREAK,
                PomodoroPhase.FOCUS, PomodoroPhase.LONG_BREAK, PomodoroPhase.FOCUS,
            ),
            phases,
        )
        assertEquals(0, s.completedFocus)
        assertEquals(3, s.totalFocus)
    }

    @Test fun `each phase gets its configured length`() {
        val brk = PomodoroMath.advance(PomodoroState(config = config))
        assertEquals(5 * 60_000L, brk.remainingMillis)
        assertEquals(25 * 60_000L, PomodoroMath.advance(brk).remainingMillis)
    }

    @Test fun `running phase counts down on the monotonic clock`() {
        val s = PomodoroState(config = config, status = PomodoroStatus.RUNNING, endElapsedMillis = 100_000)
        assertEquals(40_000, PomodoroMath.remaining(s, 60_000))
        assertEquals(0, PomodoroMath.remaining(s, 200_000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid configuration is rejected`() {
        PomodoroConfig(focusMinutes = 0).validate()
    }
}
