package com.rcb.wake.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmTimeCalculatorTest {
    private val colombo = ZoneId.of("Asia/Colombo")
    private val newYork = ZoneId.of("America/New_York")

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int, zone: ZoneId = colombo) =
        ZonedDateTime.of(y, mo, d, h, mi, 0, 0, zone)

    private fun mask(vararg days: DayOfWeek) = days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }

    @Test fun `one-time alarm later today rings today`() {
        val now = at(2026, 11, 20, 6, 0)
        assertEquals(at(2026, 11, 20, 7, 30), AlarmTimeCalculator.nextOccurrence(now, 7, 30, 0))
    }

    @Test fun `one-time alarm earlier than now rings tomorrow`() {
        val now = at(2026, 11, 20, 8, 0)
        assertEquals(at(2026, 11, 21, 7, 30), AlarmTimeCalculator.nextOccurrence(now, 7, 30, 0))
    }

    @Test fun `alarm set for the current minute moves to tomorrow`() {
        val now = at(2026, 11, 20, 7, 30)
        assertEquals(at(2026, 11, 21, 7, 30), AlarmTimeCalculator.nextOccurrence(now, 7, 30, 0))
    }

    @Test fun `midnight alarm just before midnight rings in minutes`() {
        val now = at(2026, 11, 20, 23, 59)
        assertEquals(at(2026, 11, 21, 0, 0), AlarmTimeCalculator.nextOccurrence(now, 0, 0, 0))
    }

    @Test fun `alarm across month and year boundary`() {
        val now = at(2026, 12, 31, 23, 30)
        assertEquals(at(2027, 1, 1, 6, 0), AlarmTimeCalculator.nextOccurrence(now, 6, 0, 0))
    }

    @Test fun `weekday alarm on friday evening skips to monday`() {
        // 2026-11-20 is a Friday.
        val now = at(2026, 11, 20, 19, 0)
        val weekdays = mask(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        assertEquals(at(2026, 11, 23, 7, 0), AlarmTimeCalculator.nextOccurrence(now, 7, 0, weekdays))
    }

    @Test fun `single weekday that already passed today rings next week`() {
        val now = at(2026, 11, 20, 9, 0)
        assertEquals(at(2026, 11, 27, 8, 0), AlarmTimeCalculator.nextOccurrence(now, 8, 0, mask(DayOfWeek.FRIDAY)))
    }

    @Test fun `weekend alarm`() {
        val now = at(2026, 11, 20, 9, 0)
        val weekend = mask(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        assertEquals(at(2026, 11, 21, 9, 0), AlarmTimeCalculator.nextOccurrence(now, 9, 0, weekend))
    }

    @Test fun `daily repeat behaves like every day`() {
        val now = at(2026, 11, 20, 9, 0)
        assertEquals(at(2026, 11, 21, 8, 0), AlarmTimeCalculator.nextOccurrence(now, 8, 0, AlarmLimits.ALL_DAYS_MASK))
    }

    @Test fun `alarm inside spring-forward gap resolves to after the gap`() {
        // US DST starts 2027-03-14 at 02:00 -> 03:00 in New York.
        val now = at(2027, 3, 13, 23, 0, newYork)
        val next = AlarmTimeCalculator.nextOccurrence(now, 2, 30, 0)
        assertEquals(at(2027, 3, 14, 3, 30, newYork), next)
    }

    @Test fun `alarm in fall-back overlap rings once at the earlier offset`() {
        // US DST ends 2026-11-01 at 02:00 -> 01:00 in New York; 01:30 occurs twice.
        val now = at(2026, 10, 31, 23, 0, newYork)
        val next = AlarmTimeCalculator.nextOccurrence(now, 1, 30, 0)
        assertEquals(-4 * 3600, next.offset.totalSeconds)
    }

    @Test fun `daily alarm keeps wall-clock time across DST change`() {
        val now = at(2026, 10, 31, 8, 0, newYork)
        val next = AlarmTimeCalculator.nextOccurrence(now, 7, 0, AlarmLimits.ALL_DAYS_MASK)
        assertEquals(7, next.hour)
        assertEquals(at(2026, 11, 1, 7, 0, newYork), next)
    }

    @Test fun `time-zone change recomputes local wall-clock time`() {
        val instant = at(2026, 11, 20, 6, 0).toInstant().toEpochMilli()
        val inColombo = AlarmTimeCalculator.nextOccurrenceMillis(instant, colombo, 7, 0, 0)
        val inNewYork = AlarmTimeCalculator.nextOccurrenceMillis(instant, newYork, 7, 0, 0)
        assertEquals(at(2026, 11, 20, 7, 0).toInstant().toEpochMilli(), inColombo)
        // 06:00 Colombo = 19:30 previous day New York, so 07:00 NY is the next day.
        assertEquals(at(2026, 11, 20, 7, 0, newYork).toInstant().toEpochMilli(), inNewYork)
    }

    @Test fun `snooze adds whole minutes and clamps out-of-range values`() {
        assertEquals(1_000L + 10 * 60_000, AlarmTimeCalculator.snoozeUntilMillis(1_000L, 10))
        assertEquals(60 * 60_000L, AlarmTimeCalculator.snoozeUntilMillis(0, 500))
        assertEquals(60_000L, AlarmTimeCalculator.snoozeUntilMillis(0, 0))
    }

    @Test fun `active snooze takes precedence over the schedule`() {
        val now = at(2026, 11, 20, 7, 0).toInstant().toEpochMilli()
        val alarm = sampleAlarm(enabled = true, snoozedUntil = now + 5 * 60_000)
        assertEquals(now + 5 * 60_000, AlarmTimeCalculator.effectiveTriggerMillis(alarm, now, colombo))
    }

    @Test fun `expired snooze falls back to the regular schedule`() {
        val now = at(2026, 11, 20, 7, 0).toInstant().toEpochMilli()
        val alarm = sampleAlarm(enabled = true, snoozedUntil = now - 1)
        assertEquals(at(2026, 11, 21, 6, 30).toInstant().toEpochMilli(), AlarmTimeCalculator.effectiveTriggerMillis(alarm, now, colombo))
    }

    @Test fun `disabled alarm is not scheduled`() {
        val now = at(2026, 11, 20, 7, 0).toInstant().toEpochMilli()
        assertNull(AlarmTimeCalculator.effectiveTriggerMillis(sampleAlarm(enabled = false), now, colombo))
    }

    @Test fun `scheduling is idempotent for the same instant`() {
        val now = at(2026, 11, 20, 7, 0).toInstant().toEpochMilli()
        val a = AlarmTimeCalculator.nextOccurrenceMillis(now, colombo, 6, 30, 0)
        val b = AlarmTimeCalculator.nextOccurrenceMillis(now, colombo, 6, 30, 0)
        assertEquals(a, b)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid time is rejected`() {
        AlarmTimeCalculator.nextOccurrence(at(2026, 11, 20, 7, 0), 24, 0, 0)
    }
}

fun sampleAlarm(
    id: Long = 1,
    enabled: Boolean = true,
    repeatDays: Int = 0,
    snoozedUntil: Long? = null,
    challengeEnabled: Boolean = false,
    difficulty: String = ChallengeDifficulty.EASY,
    questions: Int = 3,
    challengeType: String = ChallengeType.MATH,
) = Alarm(
    id = id, hour = 6, minute = 30, enabled = enabled, repeatDays = repeatDays, label = "", soundUri = "builtin:dawn",
    soundName = "Dawn", vibrationEnabled = true, gradualVolumeEnabled = true, snoozeMinutes = 10,
    challengeEnabled = challengeEnabled, challengeDifficulty = difficulty, challengeQuestionCount = questions, challengeType = challengeType,
    createdAt = 0, updatedAt = 0, snoozedUntil = snoozedUntil, nextTriggerAt = null, scheduleState = ScheduleState.NONE,
)
