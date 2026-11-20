package com.rcb.wake.alarm

/** Alarm configuration as received from the UI, before validation. */
data class AlarmInput(
    val id: Long?,
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
    val challengeType: String = ChallengeType.MATH,
)

class InvalidAlarmException(message: String) : IllegalArgumentException(message)

object AlarmValidator {
    private val allowedSoundSchemes = listOf("builtin:", "system:", "content://")

    /** Throws [InvalidAlarmException] if any field is out of range. Returns a normalised copy. */
    fun validate(input: AlarmInput): AlarmInput {
        if (input.hour !in 0..23) throw InvalidAlarmException("hour must be 0-23")
        if (input.minute !in 0..59) throw InvalidAlarmException("minute must be 0-59")
        if (input.repeatDays !in 0..AlarmLimits.ALL_DAYS_MASK) throw InvalidAlarmException("repeatDays out of range")
        if (input.snoozeMinutes !in AlarmLimits.MIN_SNOOZE_MINUTES..AlarmLimits.MAX_SNOOZE_MINUTES) {
            throw InvalidAlarmException("snoozeMinutes must be 1-60")
        }
        if (input.challengeDifficulty !in ChallengeDifficulty.ALL) throw InvalidAlarmException("unknown difficulty")
        if (input.challengeQuestionCount !in AlarmLimits.MIN_QUESTIONS..AlarmLimits.MAX_QUESTIONS) {
            throw InvalidAlarmException("challengeQuestionCount must be 1-10")
        }
        if (input.challengeType !in ChallengeType.ALL) throw InvalidAlarmException("unknown challenge type")
        if (allowedSoundSchemes.none { input.soundUri.startsWith(it) }) throw InvalidAlarmException("unsupported sound uri")
        if (input.id != null && input.id <= 0) throw InvalidAlarmException("invalid id")
        return input.copy(
            label = input.label.trim().take(AlarmLimits.MAX_LABEL_LENGTH),
            soundName = input.soundName.trim().take(120),
        )
    }
}
