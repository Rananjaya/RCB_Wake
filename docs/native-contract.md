# Native module contract

The TurboModule `RcbWake` is the only interface between JavaScript and the engine. Its typed
specification is [`src/native/NativeRcbWake.ts`](../src/native/NativeRcbWake.ts); React Native
codegen generates `NativeRcbWakeSpec.java`, implemented by `RcbWakeModule.kt`.

All inputs are validated in Kotlin. Invalid input rejects the promise with `E_INVALID`; other
failures reject with `E_ENGINE`. Synchronous methods are marked *(sync)*.

## Types

| Type | Fields |
| --- | --- |
| `AlarmConfig` | `id` (0 = new), `hour` 0–23, `minute` 0–59, `enabled`, `repeatDays` (bit 0 = Mon … bit 6 = Sun, 0 = once), `label` (≤ 60), `soundUri` (`builtin:<key>` · `system:default` · `content://…`), `soundName`, `vibrationEnabled`, `gradualVolumeEnabled`, `snoozeMinutes` 1–60, `challengeEnabled`, `challengeDifficulty` (`easy`·`medium`·`hard`), `challengeQuestionCount` 1–10 |
| `AlarmRecord` | `AlarmConfig` fields + `createdAt`, `updatedAt`, `snoozedUntil` (0 = none), `nextTriggerAt` (0 = not scheduled), `scheduleState` (`NONE`·`EXACT`·`INEXACT`·`FAILED`·`RINGING`) |
| `ScheduledAlarm` | `alarmId`, `triggerAt`, `scheduleState`, `registered` (PendingIntent exists) |
| `EngineStatus` | `exactAlarmPermitted`, `notificationsPermitted`, `fullScreenIntentPermitted`, `sdkInt`, `versionName`, `versionCode`, `scheduledCount`, `failedCount` |
| `SessionInfo` | `alarmIds`, `labels`, `startedAt`, `challengeRequired`, `challengeDifficulty`, `challengeQuestionCount` |
| `TimerRecord` | `id`, `label`, `durationMillis`, `remainingMillis`, `endElapsedMillis` (elapsedRealtime; 0 unless running), `status` (`IDLE`·`RUNNING`·`PAUSED`·`FINISHED`), `createdAt` |
| `PomodoroConfig` | `focusMinutes` 1–180, `shortBreakMinutes` 1–60, `longBreakMinutes` 1–120, `roundsBeforeLongBreak` 1–12, `autoStartNext` |
| `PomodoroState` | `config`, `status` (`IDLE`·`RUNNING`·`PAUSED`), `phase` (`FOCUS`·`SHORT_BREAK`·`LONG_BREAK`), `completedFocus`, `totalFocus`, `remainingMillis`, `endElapsedMillis` |
| `WorldClockRecord` | `id`, `cityName`, `zoneId` (IANA), `displayOrder` |
| `ZoneOffset` | `zoneId`, `offsetMillis`, `nextTransitionAt` (0 = none) |
| `Settings` | `theme` (`midnight`·`pink`), `accentColor` (`default`·`purple`·`blue`·`teal`·`green`·`orange`·`rose`), `timeFormat` (`12`·`24`), `language`, alarm defaults (`defaultSoundUri`, `defaultSoundName`, `defaultSnoozeMinutes`, `defaultVibration`, `defaultGradualVolume`, `defaultChallengeEnabled`, `defaultChallengeDifficulty`, `defaultChallengeQuestionCount`), weather (`weatherEnabled`, `weatherCity`, `weatherUseLocation`, `weatherLatitude`, `weatherLongitude`, `temperatureUnit` `c`·`f`), sleep (`sleepReminderEnabled`, `sleepGoalMinutes` 240–720, `sleepReminderLeadMinutes` 0–120) |
| `WeatherInfo` | `city` (empty = device location), `temperatureCelsius`, `condition`, `isDay`, `fetchedAt` |

## Methods

### Engine and permissions

| Method | Returns | Notes |
| --- | --- | --- |
| `initializeEngine()` | `EngineStatus` | Creates notification channels and reconciles alarms, timers and Pomodoro. Called on app start. |
| `getAlarmEngineStatus()` | `EngineStatus` | |
| `requestExactAlarmPermission()` | void | Opens the system screen on API 31+. |
| `requestFullScreenIntentPermission()` | void | Opens the system screen on API 34+. |
| `openNotificationSettings()` | void | |

### Alarms

| Method | Returns | Notes |
| --- | --- | --- |
| `getAlarms()` | `AlarmRecord[]` | |
| `saveAlarm(config)` | `AlarmRecord` | Create or update, then schedule. Covers the spec's `scheduleAlarm` and `updateAlarm`. Clears any pending snooze. |
| `setAlarmEnabled(id, enabled)` | `AlarmRecord` | |
| `deleteAlarm(id)` | void | Cancels the registration first (spec `cancelAlarm`). |
| `cancelAllAlarms()` | void | Disables and cancels every alarm. |
| `getScheduledAlarms()` | `ScheduledAlarm[]` | Includes whether AlarmManager actually holds the PendingIntent. |
| `getNextAlarm()` | `AlarmRecord \| null` | |

### Ringing session

| Method | Returns | Notes |
| --- | --- | --- |
| `getActiveSession()` | `SessionInfo \| null` | |
| `snoozeSession()` | number | 1 if a session was snoozed, 0 otherwise. Spec `scheduleSnooze`. |
| `dismissSession(method)` | boolean | `normal` (refused while a challenge is required), `challenge`, `emergency`. Spec `dismissAlarm`. |

### Sounds and pickers

`getBuiltInSounds()`, `pickCustomSound()` (Storage Access Framework, persistable read permission),
`previewSound(uri)`, `stopPreview()`, `showTimePicker(hour, minute, is24Hour)` → `{hour, minute, cancelled}`.

### Timers and Pomodoro

`getTimers()`, `createTimer(label, durationMillis, start)` (1 s – 99:59:59), `startTimer(id)`,
`pauseTimer(id)`, `resetTimer(id)`, `addTimerTime(id, millis)` (1 s – 1 h), `deleteTimer(id)`.

`getPomodoro()`, `startPomodoro()`, `pausePomodoro()`, `skipPomodoroPhase()`, `resetPomodoro()`,
`savePomodoroConfig(config)`.

### Clocks

`elapsedRealtime()` *(sync)* — `SystemClock.elapsedRealtime()`, used for stopwatch and countdown
display. `getBootCount()` *(sync)*.

### World clock

`getWorldClocks()`, `addWorldClock(cityName, zoneId)` (rejects unknown IANA ids, ignores exact
duplicates), `removeWorldClock(id)`, `getZoneOffsets(zoneIds, atMillis)` (pass `local` for the
device's current zone).

### Settings, small state and weather

`getSettings()`, `saveSettings(settings)` (full replace, validated), `getValue(key)`,
`setValue(key, value)`, `removeValue(key)` (keys `[A-Za-z0-9_.-]{1,64}`, values ≤ 64 000 chars),
`getWeather()`, `refreshWeather()`, `updateLocation()`.

## Events

`onEngineEvent` (codegen `EventEmitter`) delivers `{type}`:

| Type | Emitted when | JS reaction |
| --- | --- | --- |
| `alarmsChanged` | any alarm saved, toggled, fired, snoozed, dismissed, reconciled | reload alarms and status |
| `timersChanged` | any timer change, including native completion | reload timers |
| `pomodoroChanged` | any Pomodoro change, including native phase change | reload Pomodoro |
| `sessionStarted` / `sessionEnded` | ringing session begins / ends | refresh session; open or close the challenge |
| `settingsChanged` | settings saved | — (widget and sleep reminder update natively) |

Events are in-process (`EngineEvents`); the engine never depends on JavaScript listening.
