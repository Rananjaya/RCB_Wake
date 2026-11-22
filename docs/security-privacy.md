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
| Latitude and longitude rounded to 2 decimals (~1 km) — of the chosen city, or of the device's coarse location if "Use my location" is on | `https://api.open-meteo.com/v1/forecast` (default) | When enabled, when the city or location changes, and at the user's refresh interval (15 min, 30 min, 1 h or 3 h; default 30 min) while a widget is on the home screen |
| The same rounded coordinates, with a User-Agent naming the app, version and project website (required by MET Norway) | `https://api.met.no/weatherapi/locationforecast/2.0/compact` — only if the user picks MET Norway | Same as above |
| The same rounded coordinates **and the user's own API key** | `https://api.openweathermap.org/data/2.5/weather` — only if the user picks OpenWeatherMap as the provider | Same as above |

With Open-Meteo no identifiers, API keys, cookies or account data are sent. With OpenWeatherMap the
user's own key is sent, so OpenWeather can associate requests with the user's OpenWeather account.
The key is stored only in the app's device-protected settings (excluded from backup) and is never
logged. Either provider, like any web server, can see the device's IP address. Turning weather off
stops all requests and deletes the cached reading.

The refresh is an inexact, non-waking `AlarmManager` alarm, so it never wakes the device; a refresh
that falls due while the device sleeps runs at the next wake-up.

### Optional location

`ACCESS_COARSE_LOCATION` is requested only when the user turns on "Use my location", never at
startup. The position is read with the platform `LocationManager` (no Google Play Services) only
while the app is open, rounded to ~1 km before it is stored, and deleted when the option is turned
off. RCB Wake does not request background or fine location.

When the user searches for a weather location, the typed text (only) is sent to
`https://geocoding-api.open-meteo.com/v1/search` so villages and suburbs outside the bundled city
list can be chosen; nothing is sent until at least two characters are typed.

When the position changes, the place name shown with the weather (village or suburb, e.g.
"Kosgama") is looked up with Android's built-in `Geocoder`. On devices with Google services the
platform sends the coarse position to Google's geocoding service for this; only the resulting
name is stored. On Android 12 and newer, approximate location is itself blurred by the system to
about 2 km, so the name can be a neighbouring village.

### Optional calendar on the widget

`READ_CALENDAR` is requested only when the user turns on Settings → Widget calendar. RCB Wake
reads upcoming events from the system calendar provider (which Google Calendar and other calendar
apps sync into) for the calendars the user selects, and shows the title and time of the next event
on the widget. Events are read on demand, never stored by RCB Wake and never transmitted.

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
| `READ_CALENDAR` | Optional next-event line on the widget | Runtime, only if the user opts in |

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
