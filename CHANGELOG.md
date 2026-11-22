# Changelog

All notable changes to RCB Wake are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

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
