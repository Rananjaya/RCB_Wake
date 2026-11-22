package com.rcb.wake.alarm

/**
 * A user-configured alarm. [repeatDays] is a bitmask where bit 0 is Monday and bit 6 is Sunday
 * (matching `DayOfWeek.value - 1`). A mask of 0 means a one-time alarm.
 */
data class Alarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    val repeatDays: Int,
    val label: String,
    val soundUri: String,
    val soundName: String,
    val vibrationEnabled: Boolean,
    val gradualVolumeEnabled: Boolean,
    val snoozeMinutes: Int,
    val challengeEnabled: Boolean,
    val challengeDifficulty: String,
    val challengeQuestionCount: Int,
    /** `math`, `qr` or `nfc`. */
    val challengeType: String,
    val createdAt: Long,
    val updatedAt: Long,
    val snoozedUntil: Long?,
    val nextTriggerAt: Long?,
    val scheduleState: ScheduleState,
) {
    val isRepeating: Boolean get() = repeatDays != 0
}

/** What the operating system actually accepted for this alarm. */
enum class ScheduleState {
    /** Not scheduled because the alarm is disabled. */
    NONE,

    /** Scheduled with AlarmManager.setAlarmClock(). */
    EXACT,

    /** Exact alarms are not permitted; scheduled inexactly and may be delayed. */
    INEXACT,

    /** The OS rejected scheduling. The alarm will not ring. */
    FAILED,

    /** Currently ringing. */
    RINGING,
}

object ChallengeDifficulty {
    const val EASY = "easy"
    const val MEDIUM = "medium"
    const val HARD = "hard"
    val ALL = setOf(EASY, MEDIUM, HARD)
}

object ChallengeType {
    const val MATH = "math"
    const val NFC = "nfc"
    const val QR = "qr"
    val ALL = setOf(MATH, NFC, QR)
}

object AlarmLimits {
    const val MIN_SNOOZE_MINUTES = 1
    const val MAX_SNOOZE_MINUTES = 60
    const val MIN_QUESTIONS = 1
    const val MAX_QUESTIONS = 10
    const val MAX_LABEL_LENGTH = 60
    const val ALL_DAYS_MASK = 0b111_1111
}
