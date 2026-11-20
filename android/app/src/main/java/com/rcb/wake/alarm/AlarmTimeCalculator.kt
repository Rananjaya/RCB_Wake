package com.rcb.wake.alarm

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure next-occurrence arithmetic. No Android dependencies so it can be unit tested on the JVM.
 *
 * Each occurrence is computed and scheduled individually; there are no fixed repeat intervals,
 * so daylight-saving shifts and time-zone changes are always honoured.
 */
object AlarmTimeCalculator {

    /** Returns true if [day] is selected in a Monday-first [repeatDays] mask. */
    fun isDaySelected(repeatDays: Int, day: DayOfWeek): Boolean =
        repeatDays and (1 shl (day.value - 1)) != 0

    /**
     * The first moment strictly after [now] at which an alarm set for [hour]:[minute] rings.
     *
     * Local times that fall inside a daylight-saving gap resolve to the instant just after the gap
     * (java.time's default behaviour), and in an overlap the earlier offset is used, so an alarm
     * rings once per day.
     */
    fun nextOccurrence(now: ZonedDateTime, hour: Int, minute: Int, repeatDays: Int): ZonedDateTime {
        require(hour in 0..23 && minute in 0..59) { "Invalid time" }
        val time = LocalTime.of(hour, minute)
        val today = now.toLocalDate()
        // Up to 7 days ahead covers every weekday; day 7 covers "today's weekday, next week".
        for (offset in 0..7) {
            val date = today.plusDays(offset.toLong())
            val candidate = ZonedDateTime.of(date, time, now.zone)
            if (!candidate.isAfter(now)) continue
            if (repeatDays == 0 || isDaySelected(repeatDays, date.dayOfWeek)) return candidate
        }
        error("Unreachable: a valid repeat mask always matches within 8 days")
    }

    fun nextOccurrenceMillis(nowMillis: Long, zone: ZoneId, hour: Int, minute: Int, repeatDays: Int): Long =
        nextOccurrence(Instant.ofEpochMilli(nowMillis).atZone(zone), hour, minute, repeatDays)
            .toInstant().toEpochMilli()

    /**
     * When the alarm should next fire, or null if it should not be scheduled.
     * An active snooze takes precedence over the regular schedule.
     */
    fun effectiveTriggerMillis(alarm: Alarm, nowMillis: Long, zone: ZoneId): Long? {
        val snooze = alarm.snoozedUntil
        if (snooze != null && snooze > nowMillis) return snooze
        if (!alarm.enabled) return null
        return nextOccurrenceMillis(nowMillis, zone, alarm.hour, alarm.minute, alarm.repeatDays)
    }

    fun snoozeUntilMillis(nowMillis: Long, snoozeMinutes: Int): Long {
        val minutes = snoozeMinutes.coerceIn(AlarmLimits.MIN_SNOOZE_MINUTES, AlarmLimits.MAX_SNOOZE_MINUTES)
        return nowMillis + minutes * 60_000L
    }
}
