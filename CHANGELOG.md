# Changelog

All notable changes to RCB Wake are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [1.0.0] — Unreleased

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
- Midnight Purple and Pink Dream themes, accent colours.
- English, Sinhala, Tamil, German, Russian, Spanish, French, Portuguese, Hindi, Chinese
  (Simplified) and Japanese.
- Bundled original alarm tones and weather icons.

### Fixed
- Timer and Pomodoro countdowns no longer jump back a second or more when started.
- Widget layout and clock size on launchers that do not report exact widget sizes (Xiaomi HyperOS).

### Security
- No analytics, ads or tracking; data excluded from backups; immutable PendingIntents;
  non-exported components.
