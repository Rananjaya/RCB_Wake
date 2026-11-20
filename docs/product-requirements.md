# Product requirements and status

**RCB Wake** — *Wake up your way.* Android-only alarm clock, timer, stopwatch and world clock.
Package `com.rcb.wake`, version 1.0.0 (100). Minimum Android 8.0 (API 26), target API 36.

Status legend: ✅ implemented and verified · 🟡 implemented, partially verified · ⏳ planned / not in 1.0.

## Core principles

| Principle | Status |
| --- | --- |
| Alarm reliability: native engine independent of JavaScript | ✅ |
| Offline-first; only network use is opt-in weather | ✅ |
| Privacy: no accounts, analytics, ads, tracking | ✅ |
| Accessibility: labels, roles, live regions, large touch targets | 🟡 not tested with TalkBack end to end |

## Features

### Alarms
| Requirement | Status |
| --- | --- |
| Create, edit, delete, enable/disable multiple alarms | ✅ |
| 12/24-hour formats | ✅ |
| Weekday repeat; one-time alarms | ✅ |
| Labels | ✅ |
| Built-in tones (4), system default, custom file via Storage Access Framework | ✅ (custom file picker not exercised on emulator) |
| Sound preview | ✅ |
| Vibration, gradual volume, snooze length per alarm | ✅ |
| Next-alarm information | ✅ |
| Exact scheduling with OS-accepted state shown to the user | ✅ |

### Ringing
| Requirement | Status |
| --- | --- |
| Native full-screen ringing activity over the lock screen | ✅ |
| Notification fallback when full-screen is unavailable | ✅ |
| Alarm audio attributes, audio focus, fallback sounds, gradual volume | 🟡 focus loss (incoming call) not tested |
| Snooze, dismiss, auto-snooze after 10 min | ✅ |
| Simultaneous alarms share one session | 🟡 unit tested; not reproduced on device |

### Wake-up math challenge
| Requirement | Status |
| --- | --- |
| Easy / medium / hard, 1–10 questions, randomised questions and answer positions | ✅ |
| Correct/incorrect feedback; alarm keeps ringing; progress survives app restart | ✅ |
| Emergency dismissal (3-second hold) | ✅ |
| QR / movement / shake challenges | ⏳ out of scope for 1.0 |

### Timer and Pomodoro
| Requirement | Status |
| --- | --- |
| Multiple countdown timers with labels, presets, pause/resume/reset/cancel, +1 min | ✅ |
| Native background completion, notification and sound | ✅ verified with process killed |
| Pomodoro focus / short break / long break with configurable lengths and auto-start | 🟡 UI and transitions verified; background phase change verified by unit tests only |

### Stopwatch
| Requirement | Status |
| --- | --- |
| Start, pause, resume, reset, laps, fastest/slowest, clear laps | ✅ |
| Monotonic clock; survives process recreation | ✅ |

### World clock
| Requirement | Status |
| --- | --- |
| Add, remove, search ~100 bundled cities (IANA zones) | ✅ |
| Local time/date, difference, day/night, DST-aware via java.time | ✅ |

### Home-screen widget (added at owner's request)
| Requirement | Status |
| --- | --- |
| Frameless resizable widget: bar, small, medium, wide, large layouts | ✅ medium layout verified on device; others by unit test of size selection |
| Time and date (TextClock), next alarm | ✅ |
| Opt-in Open-Meteo weather, original 3D-style icons, °C/°F | ✅ |
| Optional coarse device location | ✅ |
| Optional next calendar event from chosen calendars (Google, local and other synced calendars) | ✅ verified with a local calendar on the emulator |

### Settings
| Requirement | Status |
| --- | --- |
| Midnight Purple (default) and Pink Dream themes | ✅ |
| Accent colours (default + 6) | ✅ |
| 12/24-hour, language selection | ✅ |
| Alarm and challenge defaults | ✅ |
| Bedtime sleep reminder | ✅ |
| Permission status and fixes | ✅ |
| About, privacy, licence | ✅ |

### Languages
| Language | Status |
| --- | --- |
| English, Sinhala, Tamil | ✅ (Sinhala/Tamil written by the developer; native-speaker review recommended) |
| German, Russian, Spanish, French, Portuguese, Hindi, Chinese (Simplified), Japanese | ✅ (developer translations; Settings stays English by design; native-speaker review recommended) |
| Arabic and other right-to-left languages | ⏳ not planned for 1.0 (owner decision) |
