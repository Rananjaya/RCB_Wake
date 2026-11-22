# Native alarm engine

Package `com.rcb.wake` under `android/app/src/main/java`. Everything here runs without React
Native.

## Scheduling

- **API.** `AlarmManager.setAlarmClock(AlarmClockInfo, operation)`. This is the API Android
  intends for user-visible alarm clocks: it fires exactly, is exempt from Doze and App Standby,
  shows the system "next alarm" indicator, and lets the receiver start a foreground service from
  the background (`ALARM_MANAGER_ALARM_CLOCK` exemption).
- **One occurrence at a time.** `AlarmTimeCalculator.nextOccurrence` computes the next instant
  with `java.time` for the device's current zone. Repeating alarms are never scheduled with a
  fixed interval; the next occurrence is registered when the current one fires.
- **Snooze** reuses the alarm's own PendingIntent with a new trigger time, so an alarm can never
  have two registrations.
- **Persist first.** Configuration is written to SQLite before calling AlarmManager; the result
  (`EXACT`, `INEXACT`, `FAILED`) and `nextTriggerAt` are written after. The UI reports exactly
  that state and never claims success for a rejected alarm.

### PendingIntent identity

| Kind | Action | Data URI | Request code |
| --- | --- | --- | --- |
| Alarm | `com.rcb.wake.action.ALARM_FIRE` | `rcbwake://alarm/<id>` | alarm id |
| Timer | `com.rcb.wake.action.TIMER_DONE` | `rcbwake://timer/<id>` | `Int.MAX_VALUE/2 + 1 + id` |
| Pomodoro | `com.rcb.wake.action.POMODORO_PHASE_END` | — | `Int.MAX_VALUE/2` |
| Sleep reminder | `com.rcb.wake.action.SLEEP_REMINDER` | — | `Int.MAX_VALUE/2 − 1` |

All PendingIntents are `FLAG_IMMUTABLE`; scheduling uses `FLAG_UPDATE_CURRENT` (idempotent
replace), cancellation looks up with `FLAG_NO_CREATE` and calls both `AlarmManager.cancel` and
`PendingIntent.cancel`.

### Exact-alarm permission

| Android | Permission | Behaviour |
| --- | --- | --- |
| 13+ (API 33+) | `USE_EXACT_ALARM` | Granted at install for alarm-clock apps; cannot be revoked. |
| 12/12L (API 31–32) | `SCHEDULE_EXACT_ALARM` (`maxSdkVersion=32`) | Special access; if denied, the alarm is scheduled with `setAndAllowWhileIdle` (state `INEXACT`) and the UI shows a banner with a link to the system setting. |
| 8–11 (API 26–30) | none | Exact alarms always allowed. |

`ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` triggers a reconcile so alarms are
upgraded to exact as soon as access is granted.

## Firing, stale triggers and missed alarms

`AlarmTriggerReceiver` → `AlarmController.onFired(id, triggerAt)`:

1. If the alarm no longer exists, or its stored `nextTriggerAt` differs from the intent's
   `triggerAt` by more than 5 s, the broadcast is **stale** and ignored.
2. Before ringing, the alarm is advanced: repeating alarms are registered for their next
   occurrence; one-time alarms are switched off. A lost ringing session therefore never loses
   future alarms.
3. `RingingService` is started.

`reconcileAll(reason)` runs on app start, `LOCKED_BOOT_COMPLETED`, `BOOT_COMPLETED`,
`TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED` and exact-alarm permission changes. For each
alarm whose scheduled time has passed without ringing (device off, clock moved forward):

- up to **10 minutes** late → ring now;
- later → post a "Missed alarm" notification; one-time alarms are switched off.

Then every alarm is re-registered (idempotent).

## Ringing session

`RingingService` is a `mediaPlayback` foreground service, Direct Boot aware.

- **One session.** Alarms that fire while another is ringing *join* the session
  (`AlarmSession.withAlarm`): one sound, one screen, one dismissal. The strictest challenge among
  them applies. A duplicate trigger for an alarm already in the session is ignored.
- **Persisted.** The session is stored in SharedPreferences. The service is `START_STICKY`; if the
  process is killed while ringing, Android restarts it and `restore()` resumes audio.
- **Screen.** The notification carries a full-screen intent to `RingingActivity`
  (shown over the lock screen). If the app is visible when the alarm fires, Android shows a
  heads-up instead of the full-screen intent, so the service also starts the activity directly.
- **Auto-snooze** after 10 minutes of ringing.
- **Dismiss rules.** `normal` dismissal is refused while a challenge is required; `challenge`
  and `emergency` (3-second hold on the challenge screen) are accepted.

On snooze/dismiss: audio and vibration stop, the wake lock is released, the session is cleared,
`SESSION_ENDED` is emitted (closing `RingingActivity` and the challenge screen), and the service
stops.

## Audio

`AlarmPlayer`:

- `USAGE_ALARM` / `CONTENT_TYPE_SONIFICATION` audio attributes (plays on the alarm stream,
  respects alarm volume, not silenced by media volume).
- Fallback chain: chosen sound → bundled "Dawn" → system default alarm. A custom file that was
  deleted, or a `content://` URI that is unreadable before first unlock, still rings.
- `onError` during playback switches to the bundled default.
- Gradual volume: quadratic ramp from 5% to 100% over 30 s.
- Requests audio focus but keeps ringing when another app's sound (such as a notification) takes
  it; pauses only during a phone call and resumes when the call ends (polled, in case the
  focus-gain callback never arrives).
- Vibration is started one pattern cycle at a time, so a notification vibration that replaces it
  (older Android versions) only interrupts one cycle.
- While an alarm is set, an ongoing silent notification on the `next_alarm` channel (low
  importance) shows the next alarm and keeps an alarm icon in the status bar. It is hidden while
  that alarm is snoozed (the snooze notification shows it) and can be turned off in the system
  notification settings.
- If no sound can be opened at all, vibration is forced on.

## Timers and Pomodoro

- Scheduled with `setExactAndAllowWhileIdle(ELAPSED_REALTIME_WAKEUP)` — monotonic, so manual
  clock and time-zone changes do not affect them.
- Both monotonic end time and wall-clock end time are stored. After a reboot (detected with
  `Settings.Global.BOOT_COUNT`) the remaining time is recovered from wall-clock time.
- A completion broadcast is ignored unless the timer is still `RUNNING` and within 1.5 s of its
  end, so duplicate or early deliveries cannot double-notify.
- Finished timers post an insistent notification on the `timer_done` channel (alarm usage sound)
  with Stop and +1 min actions.
- Pomodoro cycles Focus → Short break → … → Long break after N focus rounds
  (`PomodoroMath.advance`), auto-starting the next phase if enabled.

## Bedtime reminder

`SleepReminder.reschedule` runs whenever alarms or settings change and after boot. Reminder time
= next alarm − sleep goal − lead time; skipped if already in the past. The receiver checks that
the next alarm is still the one the reminder was computed for.

## Direct Boot

The ringing service and activity, all receivers, and storage are Direct Boot aware.
`MainApplication` loads React Native only after the user unlocks (it registers for
`ACTION_USER_UNLOCKED` when started locked).

## Known platform limitations

- **Force stop.** If the user force-stops RCB Wake from system settings, Android cancels all of its
  alarms and blocks its receivers until the app is opened again. This is Android behaviour and
  cannot be worked around; the Settings screen explains it.
- **OEM battery managers.** Some manufacturers add aggressive background restrictions beyond
  stock Android. `setAlarmClock` is the most robust API available, but behaviour on such devices
  has not been tested (see [testing.md](testing.md)).
- **Full-screen intent on Android 14+.** If the user revokes "full screen notifications", the alarm
  still rings with a high-priority notification; the app shows a banner to restore it.
