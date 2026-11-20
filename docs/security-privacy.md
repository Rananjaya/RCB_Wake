# Security and privacy

## Privacy summary

- No accounts, backend, analytics, advertising or tracking SDKs.
- Alarms, timers, cities, settings and stopwatch data never leave the device and are excluded
  from cloud backup and device transfer.
- Custom alarm sounds are opened through the Storage Access Framework with a persistable read
  grant. Files are never copied or uploaded; only the URI and display name are stored.
- Alarm labels and file names are never written to logs.

### The only network use: opt-in widget weather

Off by default. When the user enables it in Settings → Widget weather:

| What is sent | To | When |
| --- | --- | --- |
| Latitude and longitude rounded to 2 decimals (~1 km) — of the chosen city, or of the device's coarse location if "Use my location" is on | `https://api.open-meteo.com/v1/forecast` | When enabled, when the city or location changes, and at most every 30 min (widget update period) |

No identifiers, API keys, cookies or account data are sent. Open-Meteo, like any web server, can
see the device's IP address. Turning weather off stops all requests and deletes the cached reading.

### Optional location

`ACCESS_COARSE_LOCATION` is requested only when the user turns on "Use my location", never at
startup. The position is read with the platform `LocationManager` (no Google Play Services) only
while the app is open, rounded to ~1 km before it is stored, and deleted when the option is turned
off. RCB Wake does not request background or fine location.

## Permissions

| Permission | Why | When granted |
| --- | --- | --- |
| `USE_EXACT_ALARM` (API 33+) | Ring alarms at the exact minute | Install |
| `SCHEDULE_EXACT_ALARM` (API 31–32 only) | Same, on Android 12/12L | Special access, user-controlled |
| `RECEIVE_BOOT_COMPLETED` | Re-register alarms after reboot | Install |
| `POST_NOTIFICATIONS` | Ringing, snooze, timer, Pomodoro, missed-alarm and bedtime notifications | Runtime (API 33+), asked on first launch |
| `USE_FULL_SCREEN_INTENT` | Show the ringing screen over the lock screen | Install; user can revoke on API 34+ |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keep alarm audio playing | Install |
| `VIBRATE` | Alarm vibration | Install |
| `WAKE_LOCK` | Keep the CPU awake while ringing | Install |
| `INTERNET` | Opt-in widget weather only | Install; unused while weather is off |
| `ACCESS_COARSE_LOCATION` | Optional "use my location" for weather | Runtime, only if the user opts in |

Removed from release builds by `src/release/AndroidManifest.xml`: `ACCESS_NETWORK_STATE`,
`ACCESS_WIFI_STATE`, and the React Native dev-tooling permissions `SYSTEM_ALERT_WINDOW` and
`ACCESS_LOCAL_NETWORK` (these appear only in debug builds).

## Component exposure

| Component | Exported | Notes |
| --- | --- | --- |
| `MainActivity` | yes | Launcher entry point |
| `SystemEventReceiver` | yes | Only for protected system broadcasts (boot, time, time zone, package replaced, exact-alarm permission) |
| `RingingActivity`, `RingingService` | no | |
| `AlarmTriggerReceiver`, `TimerReceiver`, `NotificationActionReceiver`, `SleepReminderReceiver` | no | Reached only through the app's own immutable PendingIntents |
| `ClockWidgetProvider` | no | Updated by the system and by the app |

Additional measures: immutable PendingIntents everywhere; input validation for every module call;
`usesCleartextTraffic` false in release; the weather client uses HTTPS only.

## Secrets and signing

- No API keys exist in the app.
- The release keystore and its passwords are kept outside the repository and supplied through
  `RCB_WAKE_SIGNING_PROPERTIES` (see [release.md](release.md)). `.gitignore` blocks
  `*.keystore`, `*.jks` and `keystore.properties`.
