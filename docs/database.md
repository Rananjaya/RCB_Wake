# Data storage

All persistent data lives on the device in **device-protected storage**
(`Context.createDeviceProtectedStorageContext()`), so the alarm engine can read it during Direct
Boot. `android:allowBackup="false"` and `data_extraction_rules.xml` exclude everything from cloud
backup and device-to-device transfer.

## SQLite — `rcb_wake.db` (schema version 1)

Owned by `WakeDatabase` (`SQLiteOpenHelper`, WAL enabled). Accessed only from Kotlin.

```mermaid
erDiagram
    alarms {
        INTEGER id PK
        INTEGER hour "0-23"
        INTEGER minute "0-59"
        INTEGER enabled "0/1"
        INTEGER repeat_days "bit0=Mon..bit6=Sun"
        TEXT label
        TEXT sound_uri
        TEXT sound_name
        INTEGER vibration_enabled
        INTEGER gradual_volume_enabled
        INTEGER snooze_minutes
        INTEGER challenge_enabled
        TEXT challenge_difficulty
        INTEGER challenge_question_count
        INTEGER created_at
        INTEGER updated_at
        INTEGER snoozed_until "nullable"
        INTEGER next_trigger_at "nullable, what AlarmManager holds"
        TEXT schedule_state "NONE|EXACT|INEXACT|FAILED|RINGING"
    }
    timers {
        INTEGER id PK
        TEXT label
        INTEGER duration_millis
        INTEGER remaining_millis
        INTEGER end_elapsed_millis "nullable, elapsedRealtime"
        INTEGER end_time_millis "nullable, wall clock, reboot recovery"
        TEXT status "IDLE|RUNNING|PAUSED|FINISHED"
        INTEGER created_at
    }
    world_clocks {
        INTEGER id PK
        TEXT city_name
        TEXT zone_id "IANA"
        INTEGER display_order
    }
```

Stable ids: `INTEGER PRIMARY KEY AUTOINCREMENT`. Alarm ids double as PendingIntent request codes.

Migrations: `onUpgrade` is empty for version 1. Future schema changes must be additive and
non-destructive.

## SharedPreferences

| File | Key | Contents |
| --- | --- | --- |
| `rcb_wake_settings` | one key per `Settings` field | User preferences (see [native-contract.md](native-contract.md)). Defaults: Midnight Purple, system time format and language (falls back to English), "Dawn" tone, 10-min snooze, vibration and gradual volume on, challenge off, weather off, sleep reminder off. |
| `rcb_wake_state` | `active_session` | Ringing session JSON (alarm ids, start time, challenge requirements). |
| | `pomodoro`, `pomodoro_boot_count` | Pomodoro state JSON and boot count. |
| | `timers_boot_count` | Boot count when timers were last scheduled. |
| | `weather_snapshot` | Last weather reading (temperature, condition, day/night, time, source). |
| | `location_lat`, `location_lon`, `location_at` | Coarse device location (2 decimals) when "use my location" is on. Cleared when it is turned off. |
| | `js.stopwatch` | Stopwatch state written by JS (monotonic times + boot count). |
| | `js.challenge_progress` | Current challenge question and progress; cleared with the session. |

## Synchronisation contract

1. JavaScript never writes storage directly; it calls the module.
2. Every engine mutation persists before scheduling and records the scheduling result.
3. The engine emits an event after each change; JS stores reload from the engine.
4. Receivers and services read the same stores, so ringing never needs JavaScript.
