# Changelog

All notable changes to RCB Wake are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [1.8.4] — 2026-11-22

### Changed
- While an event or alarm is "Soon", My Day pages switch automatically even when automatic
  switching is turned off; it stops again when the event or alarm is over.

## [1.8.3] — 2026-11-22

### Changed
- Automatic switching of My Day pages is now off by default; tap to switch, or turn it on in
  Settings → Home-screen widget.

## [1.8.2] — 2026-11-22

### Changed
- While something is "Soon", My Day keeps switching pages automatically if that setting is on;
  "Soon" ends when the event or alarm is over.

## [1.8.1] — 2026-11-22

### Changed
- While an event or alarm is "Soon", the My Day widget jumps to it once, but you can still tap to
  switch pages.

## [1.8.0] — 2026-11-22

### Added
- MET Norway (api.met.no) as a weather provider — free, no API key, data CC BY 4.0.
- "Refresh events" in Settings → Widget calendar: asks synced calendars (e.g. Google) to sync
  now and updates the widgets and reminders.

### Fixed
- New or changed calendar events now show up automatically: the calendar is re-read every
  30 minutes (or at the weather interval when weather is on), without waking the phone.

## [1.7.1] — 2026-11-22

### Changed
- My Day "Soon" page shows only what is soon: the alarm, the event, or both. Connected earphones
  stay visible.

## [1.7.0] — 2026-11-22

### Added
- Reminders: a small notification before the next alarm (silent) and before timed calendar
  events, 5, 10, 15 or 30 minutes ahead (Settings → Reminders; on by default, 10 minutes).

### Fixed
- Widget text colour now changes back to white after using Material You or the accent colour.
- While an event or alarm is "soon", the My Day widget no longer keeps switching pages.
- The calendar line refreshes when only the My Day widget is on the home screen.

## [1.6.0] — 2026-11-22

### Added
- My Day pages switch automatically every few seconds (can be turned off).
- Within 10 minutes of a calendar event or an alarm, My Day shows "Soon" with it and stops
  switching until it has started (events: until they end).
- Double-tap My Day to open the app; a single tap still switches the page.

### Fixed
- The page dots no longer disappear when the event line is shown; they now sit on the right.

## [1.5.0] — 2026-11-22

### Changed
- My Day widget now has two pages: page 1 shows the date with year and the weather (and the
  greeting if enabled); tap the widget to see page 2 — next event, next alarm and connected
  earphones. Page dots show the current page. (Home-screen widgets cannot receive swipes, so
  pages are switched by tapping.)

## [1.4.1] — 2026-11-22

### Changed
- My Day widget: one item per line — date, weather, next event, then earphones.

## [1.4.0] — 2026-11-22

### Added
- Setting to turn the My Day greeting on or off; when off, the date is shown large instead.

## [1.3.0] — 2026-11-22

### Added
- My Day widget can show connected wired or Bluetooth earphones, with the Bluetooth battery
  level (opt-in; uses the Nearby devices permission on Android 12+).
- "Add to home screen" buttons for both widgets in Settings.

## [1.2.0] — 2026-11-22

### Added
- "My Day" home-screen widget: a greeting that changes through the day (good morning,
  afternoon, evening, night), the date with the current weather icon and temperature, and the next
  calendar event. Uses the same weather, text-colour and place-name settings as the clock widget.

## [1.1.1] — 2026-11-22

### Changed
- Widget date, temperature, place name, next alarm and next event use a slightly bolder (medium)
  weight of the system font for better readability.

## [1.1.0] — 2026-11-22

### Changed
- New colourful weather icons for the widget (original artwork): scalloped sun, soft layered
  clouds, blue moon, teardrop rain, snowflakes and a gold lightning bolt, with day and night
  versions.

### Added
- Weather conditions mostly clear, drizzle, heavy rain and sleet, and a day/night thunderstorm icon.

## [1.0.1] — 2026-11-22

### Fixed
- The alarm challenge screen no longer stays open, and the app no longer shows over the lock
  screen, after an alarm has ended.

### Changed
- Math and QR code are now two separate wake-up challenges; choose one per alarm.

## [1.0.0] — 2026-11-22

### Added
- Native Kotlin alarm engine: exact `setAlarmClock` scheduling, weekday and one-time alarms,
  snooze, dismissal, missed-alarm handling, reboot / Direct Boot, time, time-zone and package
  update recovery.
- Full-screen native ringing screen and foreground ringing service with fallback sounds,
  gradual volume, vibration, audio focus and auto-snooze.
- Wake-up math challenge (easy / medium / hard) with emergency dismiss.
- Multiple countdown timers with native background completion.
- Pomodoro focus timer.
- Stopwatch with laps.
- World clock with about 100 cities.
- Bedtime sleep reminder.
- Home-screen clock widget with optional Open-Meteo weather and optional coarse location.
- Optional OpenWeatherMap weather with the user's own API key, and a selectable weather refresh
  interval (15 min, 30 min, 1 h, 3 h).
- Widget text colour: white, Material You (Android 12+) or the app accent.
- "Next alarm" status-bar notification while an alarm is set.
- Weather by location shows the village or suburb name, in Settings and on the wide and large widget;
  it can be turned off in Settings → Home-screen widget. Any village or suburb can be searched
  (Open-Meteo geocoding) and chosen as the weather location. Settings shows the exact coordinates sent to the weather provider and has a "Refresh now" button.
- Midnight Purple and Pink Dream themes, accent colours.
- English, Sinhala, Tamil, German, Russian, Spanish, French, Portuguese, Hindi, Chinese
  (Simplified) and Japanese.
- Bundled original alarm tones and weather icons.

### Fixed
- A ringing alarm no longer goes silent, or stops vibrating, when another app's notification arrives.
- Timer and Pomodoro countdowns no longer jump back a second or more when started.
- Widget layout and clock size on launchers that do not report exact widget sizes (Xiaomi HyperOS).

### Security
- No analytics, ads or tracking; data excluded from backups; immutable PendingIntents;
  non-exported components.
