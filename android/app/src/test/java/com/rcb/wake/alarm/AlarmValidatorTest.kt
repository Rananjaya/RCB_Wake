package com.rcb.wake.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmValidatorTest {
    private val valid = AlarmInput(
        id = null, hour = 7, minute = 0, enabled = true, repeatDays = 0b0011111, label = "  Gym  ",
        soundUri = "builtin:dawn", soundName = "Dawn", vibrationEnabled = true, gradualVolumeEnabled = false,
        snoozeMinutes = 10, challengeEnabled = true, challengeDifficulty = "medium", challengeQuestionCount = 3,
    )

    @Test fun `valid input is normalised`() {
        assertEquals("Gym", AlarmValidator.validate(valid).label)
    }

    @Test fun `long labels are truncated`() {
        assertEquals(AlarmLimits.MAX_LABEL_LENGTH, AlarmValidator.validate(valid.copy(label = "x".repeat(200))).label.length)
    }

    @Test fun `content and system sounds are accepted`() {
        AlarmValidator.validate(valid.copy(soundUri = "content://media/1"))
        AlarmValidator.validate(valid.copy(soundUri = "system:default"))
    }

    @Test(expected = InvalidAlarmException::class) fun `hour out of range`() { AlarmValidator.validate(valid.copy(hour = 24)) }
    @Test(expected = InvalidAlarmException::class) fun `minute out of range`() { AlarmValidator.validate(valid.copy(minute = -1)) }
    @Test(expected = InvalidAlarmException::class) fun `repeat mask out of range`() { AlarmValidator.validate(valid.copy(repeatDays = 128)) }
    @Test(expected = InvalidAlarmException::class) fun `snooze out of range`() { AlarmValidator.validate(valid.copy(snoozeMinutes = 0)) }
    @Test(expected = InvalidAlarmException::class) fun `unknown difficulty`() { AlarmValidator.validate(valid.copy(challengeDifficulty = "insane")) }
    @Test(expected = InvalidAlarmException::class) fun `too many questions`() { AlarmValidator.validate(valid.copy(challengeQuestionCount = 11)) }
    @Test(expected = InvalidAlarmException::class) fun `file uris are rejected`() { AlarmValidator.validate(valid.copy(soundUri = "file:///sdcard/a.mp3")) }
    @Test(expected = InvalidAlarmException::class) fun `non-positive id`() { AlarmValidator.validate(valid.copy(id = 0)) }
}
