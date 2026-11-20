package com.rcb.wake.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * The single persistent store for alarms, timers and world clocks.
 *
 * Lives in device-protected storage so the alarm engine can read it during Direct Boot
 * (after a reboot, before the user unlocks). React Native never opens this database directly;
 * all reads and writes go through the native module so there is exactly one source of truth.
 */
class WakeDatabase private constructor(context: Context) :
    SQLiteOpenHelper(AppStorage.deviceContext(context), DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE alarms (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                enabled INTEGER NOT NULL,
                repeat_days INTEGER NOT NULL,
                label TEXT NOT NULL,
                sound_uri TEXT NOT NULL,
                sound_name TEXT NOT NULL,
                vibration_enabled INTEGER NOT NULL,
                gradual_volume_enabled INTEGER NOT NULL,
                snooze_minutes INTEGER NOT NULL,
                challenge_enabled INTEGER NOT NULL,
                challenge_difficulty TEXT NOT NULL,
                challenge_question_count INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                snoozed_until INTEGER,
                next_trigger_at INTEGER,
                schedule_state TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE timers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                label TEXT NOT NULL,
                duration_millis INTEGER NOT NULL,
                remaining_millis INTEGER NOT NULL,
                end_elapsed_millis INTEGER,
                end_time_millis INTEGER,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE world_clocks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                city_name TEXT NOT NULL,
                zone_id TEXT NOT NULL,
                display_order INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 is the first schema. Future migrations are added here, never destructive.
    }

    companion object {
        private const val DB_NAME = "rcb_wake.db"
        private const val DB_VERSION = 1

        @Volatile private var instance: WakeDatabase? = null

        fun get(context: Context): WakeDatabase =
            instance ?: synchronized(this) {
                instance ?: WakeDatabase(context.applicationContext).also { instance = it }
            }
    }
}
