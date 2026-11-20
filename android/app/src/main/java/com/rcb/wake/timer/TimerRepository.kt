package com.rcb.wake.timer

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.rcb.wake.storage.WakeDatabase

class TimerRepository(context: Context) {
    private val db = WakeDatabase.get(context)

    fun getAll(): List<WakeTimer> =
        db.readableDatabase.query("timers", null, null, null, null, null, "created_at, id").use { c ->
            buildList { while (c.moveToNext()) add(c.toTimer()) }
        }

    fun get(id: Long): WakeTimer? =
        db.readableDatabase.query("timers", null, "id = ?", arrayOf(id.toString()), null, null, null).use { c ->
            if (c.moveToFirst()) c.toTimer() else null
        }

    fun insert(label: String, durationMillis: Long, now: Long): Long =
        db.writableDatabase.insertOrThrow(
            "timers",
            null,
            ContentValues().apply {
                put("label", label)
                put("duration_millis", durationMillis)
                put("remaining_millis", durationMillis)
                putNull("end_elapsed_millis")
                putNull("end_time_millis")
                put("status", TimerStatus.IDLE.name)
                put("created_at", now)
            },
        )

    fun update(timer: WakeTimer) {
        val values = ContentValues().apply {
            put("label", timer.label)
            put("duration_millis", timer.durationMillis)
            put("remaining_millis", timer.remainingMillis)
            if (timer.endElapsedMillis == null) putNull("end_elapsed_millis") else put("end_elapsed_millis", timer.endElapsedMillis)
            if (timer.endTimeMillis == null) putNull("end_time_millis") else put("end_time_millis", timer.endTimeMillis)
            put("status", timer.status.name)
        }
        db.writableDatabase.update("timers", values, "id = ?", arrayOf(timer.id.toString()))
    }

    fun delete(id: Long) {
        db.writableDatabase.delete("timers", "id = ?", arrayOf(id.toString()))
    }

    private fun Cursor.toTimer(): WakeTimer {
        fun ln(n: String): Long? = getColumnIndexOrThrow(n).let { if (isNull(it)) null else getLong(it) }
        return WakeTimer(
            id = getLong(getColumnIndexOrThrow("id")),
            label = getString(getColumnIndexOrThrow("label")),
            durationMillis = getLong(getColumnIndexOrThrow("duration_millis")),
            remainingMillis = getLong(getColumnIndexOrThrow("remaining_millis")),
            endElapsedMillis = ln("end_elapsed_millis"),
            endTimeMillis = ln("end_time_millis"),
            status = runCatching { TimerStatus.valueOf(getString(getColumnIndexOrThrow("status"))) }.getOrDefault(TimerStatus.IDLE),
            createdAt = getLong(getColumnIndexOrThrow("created_at")),
        )
    }
}
