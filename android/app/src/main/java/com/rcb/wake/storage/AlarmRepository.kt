package com.rcb.wake.storage

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.rcb.wake.alarm.Alarm
import com.rcb.wake.alarm.AlarmInput
import com.rcb.wake.alarm.ScheduleState

class AlarmRepository(context: Context) {
    private val db = WakeDatabase.get(context)

    fun getAll(): List<Alarm> =
        db.readableDatabase.query("alarms", null, null, null, null, null, "hour, minute, id").use { c ->
            buildList { while (c.moveToNext()) add(c.toAlarm()) }
        }

    fun get(id: Long): Alarm? =
        db.readableDatabase.query("alarms", null, "id = ?", arrayOf(id.toString()), null, null, null).use { c ->
            if (c.moveToFirst()) c.toAlarm() else null
        }

    /** Inserts or updates the alarm configuration. Scheduling state is preserved on update. */
    fun upsert(input: AlarmInput, now: Long): Long {
        val values = ContentValues().apply {
            put("hour", input.hour)
            put("minute", input.minute)
            put("enabled", input.enabled.toInt())
            put("repeat_days", input.repeatDays)
            put("label", input.label)
            put("sound_uri", input.soundUri)
            put("sound_name", input.soundName)
            put("vibration_enabled", input.vibrationEnabled.toInt())
            put("gradual_volume_enabled", input.gradualVolumeEnabled.toInt())
            put("snooze_minutes", input.snoozeMinutes)
            put("challenge_enabled", input.challengeEnabled.toInt())
            put("challenge_difficulty", input.challengeDifficulty)
            put("challenge_question_count", input.challengeQuestionCount)
            put("updated_at", now)
            // Editing an alarm always cancels a pending snooze.
            putNull("snoozed_until")
        }
        val w = db.writableDatabase
        if (input.id == null) {
            values.put("created_at", now)
            values.put("schedule_state", ScheduleState.NONE.name)
            return w.insertOrThrow("alarms", null, values)
        }
        val rows = w.update("alarms", values, "id = ?", arrayOf(input.id.toString()))
        require(rows == 1) { "Alarm not found" }
        return input.id
    }

    fun setEnabled(id: Long, enabled: Boolean, now: Long) {
        val values = ContentValues().apply {
            put("enabled", enabled.toInt())
            put("updated_at", now)
            putNull("snoozed_until")
        }
        db.writableDatabase.update("alarms", values, "id = ?", arrayOf(id.toString()))
    }

    fun setSnoozedUntil(id: Long, snoozedUntil: Long?) {
        val values = ContentValues().apply {
            if (snoozedUntil == null) putNull("snoozed_until") else put("snoozed_until", snoozedUntil)
        }
        db.writableDatabase.update("alarms", values, "id = ?", arrayOf(id.toString()))
    }

    fun setScheduleResult(id: Long, nextTriggerAt: Long?, state: ScheduleState) {
        val values = ContentValues().apply {
            if (nextTriggerAt == null) putNull("next_trigger_at") else put("next_trigger_at", nextTriggerAt)
            put("schedule_state", state.name)
        }
        db.writableDatabase.update("alarms", values, "id = ?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        db.writableDatabase.delete("alarms", "id = ?", arrayOf(id.toString()))
    }

    private fun Boolean.toInt() = if (this) 1 else 0

    private fun Cursor.toAlarm(): Alarm {
        fun s(n: String) = getString(getColumnIndexOrThrow(n))
        fun i(n: String) = getInt(getColumnIndexOrThrow(n))
        fun l(n: String) = getLong(getColumnIndexOrThrow(n))
        fun ln(n: String): Long? = getColumnIndexOrThrow(n).let { if (isNull(it)) null else getLong(it) }
        return Alarm(
            id = l("id"),
            hour = i("hour"),
            minute = i("minute"),
            enabled = i("enabled") == 1,
            repeatDays = i("repeat_days"),
            label = s("label"),
            soundUri = s("sound_uri"),
            soundName = s("sound_name"),
            vibrationEnabled = i("vibration_enabled") == 1,
            gradualVolumeEnabled = i("gradual_volume_enabled") == 1,
            snoozeMinutes = i("snooze_minutes"),
            challengeEnabled = i("challenge_enabled") == 1,
            challengeDifficulty = s("challenge_difficulty"),
            challengeQuestionCount = i("challenge_question_count"),
            createdAt = l("created_at"),
            updatedAt = l("updated_at"),
            snoozedUntil = ln("snoozed_until"),
            nextTriggerAt = ln("next_trigger_at"),
            scheduleState = runCatching { ScheduleState.valueOf(s("schedule_state")) }.getOrDefault(ScheduleState.NONE),
        )
    }
}
