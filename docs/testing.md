# Testing

Three kinds of evidence are kept separate below: **automated tests**, **emulator tests** and
**physical-device tests**. Results are from 20 November 2026.

## Automated tests

```sh
yarn test           # Jest
yarn test:kotlin    # JVM unit tests (./gradlew :app:testDebugUnitTest)
yarn typecheck && yarn lint
```

### Kotlin (JVM) — 55 tests, all passing

| Suite | Covers |
| --- | --- |
| `AlarmTimeCalculatorTest` | one-time today/tomorrow, current minute, midnight, month/year rollover, weekdays across the weekend, single weekday next week, weekends, every day, DST spring-forward gap, DST fall-back overlap, wall-clock time kept across DST, time-zone change, snooze arithmetic and clamping, snooze precedence, expired snooze, disabled alarm, idempotency, invalid input |
| `AlarmValidatorTest` | label trimming/truncation, accepted sound URIs, every out-of-range field, `file://` rejection |
| `AlarmSessionTest` | duplicate triggers, simultaneous alarms joining a session, challenge merging (strictest wins) |
| `TimerMathTest` | monotonic countdown, overrun, paused/finished, reboot recovery from wall clock, immunity to wall-clock jumps, duration limits |
| `PomodoroMathTest` | phase sequence and long break after N rounds, phase lengths, monotonic countdown, config validation |
| `SleepMathTest` | reminder time, past bedtime, no alarm, bedtime |
| `WeatherCodesTest` | WMO code mapping incl. showers, °C/°F formatting |
| `WidgetLayoutTest` | layout selection by widget size |

### Jest — 55 tests, all passing

| Suite | Covers |
| --- | --- |
| `time.test.ts` | 12/24-hour formatting, repeat-day bitmask, repeat summaries, time-until, countdown and stopwatch formatting, accent colours |
| `mathChallenge.test.ts` | 300 generated questions per difficulty: correct answers, four distinct non-negative options, answer position varies; progress restore |
| `stopwatch.test.ts` | start/pause/resume, laps, fastest/slowest, reset, clear laps, reboot restore |
| `timerEntry.test.ts` | keypad entry, carry-over, monotonic display |
| `worldClock.test.ts` | zone time, day offset, day/night, half-hour offsets, city data integrity, search |
| `i18n.test.ts` | every language has every key and the same placeholders |
| `stores.test.ts` | alarm create/edit/toggle/delete through the engine, defaults, multiple timers, settings persistence, Pomodoro cycle |
| `sleepReminder.test.ts` | reminder timing and goal formatting |

A real bug found by these tests: hard challenge questions could have negative answers (fixed).

## Emulator tests

Android emulators: **Pixel 8, Android 15 (API 35)** and **Pixel 9a, Android 17 (API 37)**, debug
builds. "Killed" means the app process was not running.

| Scenario | Result |
| --- | --- |
| Alarm accepted by AlarmManager as exact alarm clock (`exactAllowReason=policy_permission`) | ✅ Pass |
| Alarm rings with app process killed and screen off (foreground service + `USAGE_ALARM` audio) | ✅ Pass |
| Full-screen ringing activity over a locked keyguard | ✅ Pass |
| Math challenge over the lock screen while audio continues; solved → service and audio stop | ✅ Pass |
| Emergency dismiss (3 s hold) | ✅ Pass |
| Dismiss from notification | ✅ Pass |
| Snooze → new registration at +10 min, audio stops; snoozed alarm rings | ✅ Pass |
| Time-zone change (Colombo → New York) → alarms keep local wall time | ✅ Pass |
| Manual clock change forward → `TIME_SET` reconcile, alarm rings on time | ✅ Pass |
| Reboot → `LOCKED_BOOT_COMPLETED` and `BOOT_COMPLETED` reconcile; alarm missed during reboot rang immediately (late-ring grace) | ✅ Pass |
| Doze | 🟡 Inconclusive — the emulator would not enter deep idle (stopped at `INACTIVE`); the alarm fired via `ALARM_MANAGER_ALARM_CLOCK` in that state |
| Timer completion after `kill -9` of the app process | ✅ Pass |
| Alarm firing while the app is open showed only a heads-up | ❌ Found → fixed (service now starts the ringing screen) |
| Widget placement, frameless rendering, weather icon, °C | ✅ Pass |
| Opt-in weather fetch from Open-Meteo (Colombo) | ✅ Pass |
| "Use my location" → coarse position stored rounded to 2 decimals, weather fetched | ✅ Pass |
| Sleep reminder registered at alarm − goal − lead | ✅ Pass |
| Pomodoro start / pause / skip / settings UI | ✅ Pass |
| Pomodoro phase change in the background | ⏳ Not completed on device (unit-tested) |
| Exact-alarm access denied (Android 12) | ⏳ Not tested (no API 31/32 emulator run) |
| Notifications denied / full-screen intent revoked | ⏳ Not tested |
| Audio interruption by a phone call | ⏳ Not tested |

## Physical-device tests

**None performed yet.** Before publishing to users, run the emulator scenarios above on at least
one stock-Android device and one device with an aggressive OEM battery manager
(e.g. Samsung, Xiaomi), overnight with the screen locked.

## Manual test checklist

1. Create an alarm 2 minutes ahead; lock the screen; kill the app (`adb shell am kill com.rcb.wake`).
2. Confirm it rings full-screen, snooze, confirm it rings again, then dismiss.
3. Repeat with a challenge; solve it; repeat and use emergency dismiss.
4. `adb shell cmd alarm set-timezone <zone>` and confirm the next alarm keeps its local time.
5. `adb reboot` with an alarm pending; confirm it is re-registered (`adb shell dumpsys alarm | grep rcb`).
6. Start a 15 s timer; `kill -9` the app; confirm the "Time's up" notification.
