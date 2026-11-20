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
- Midnight Purple and Pink Dream themes, accent colours.
- English, Sinhala, Tamil, German, Russian, Spanish, French, Portuguese, Hindi, Chinese
  (Simplified) and Japanese.
- Bundled original alarm tones and weather icons.

### Security
- No analytics, ads or tracking; data excluded from backups; immutable PendingIntents;
  non-exported components.
