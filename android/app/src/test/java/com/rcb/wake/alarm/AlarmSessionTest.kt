package com.rcb.wake.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSessionTest {
    @Test fun `duplicate trigger of the same alarm does not change the session`() {
        val session = AlarmSession.start(sampleAlarm(id = 1), now = 100)
        assertSame(session, session.withAlarm(sampleAlarm(id = 1)))
    }

    @Test fun `simultaneous alarms join one session`() {
        val session = AlarmSession.start(sampleAlarm(id = 1), now = 100).withAlarm(sampleAlarm(id = 2))
        assertEquals(listOf(1L, 2L), session.alarmIds)
        assertEquals(100, session.startedAt)
    }

    @Test fun `joining alarm with challenge makes the session require it`() {
        val session = AlarmSession.start(sampleAlarm(id = 1), 0)
            .withAlarm(sampleAlarm(id = 2, challengeEnabled = true, difficulty = ChallengeDifficulty.HARD, questions = 5))
        assertTrue(session.challengeRequired)
        assertEquals(ChallengeDifficulty.HARD, session.challengeDifficulty)
        assertEquals(5, session.challengeQuestionCount)
    }

    @Test fun `strictest challenge wins`() {
        val session = AlarmSession.start(sampleAlarm(id = 1, challengeEnabled = true, difficulty = ChallengeDifficulty.HARD, questions = 2), 0)
            .withAlarm(sampleAlarm(id = 2, challengeEnabled = true, difficulty = ChallengeDifficulty.EASY, questions = 6))
        assertEquals(ChallengeDifficulty.HARD, session.challengeDifficulty)
        assertEquals(6, session.challengeQuestionCount)
    }

    @Test fun `session without challenge`() {
        assertFalse(AlarmSession.start(sampleAlarm(), 0).challengeRequired)
    }
}
