# RCB Wake — Google Play release kit (1.0.0)

Everything needed to publish RCB Wake 1.0.0 (`com.rcb.wake`, versionCode 100) on Google Play.
All screenshots are real captures from the app running on a Samsung Galaxy A52 (Android 13),
with only the system status and navigation bars cropped off. No UI was redrawn or edited.

## Assets

| File | Size | Upload to (Play Console → Main store listing) |
| --- | --- | --- |
| `icon/play-store-icon-512.png` | 512 × 512, 32-bit PNG | App icon |
| `feature-graphic/feature-graphic-1024x500.png` | 1024 × 500, PNG, no alpha | Feature graphic |
| `screenshots/01-alarm-dashboard.png` … `09-themes.png` (8 files) | 1080 × 1920 (9:16) | Phone screenshots, in this order |
| `promotional/pink-dream-promo.png` | 1080 × 1920 | Optional extra / social media |
| `promotional/06-stopwatch.png` | 1080 × 1920 | Optional (Play accepts 8 phone screenshots) |
| `store-listing.md` | — | App name, short and full description, release notes |

Not included: a home-screen widget screenshot (it could not be captured without a third-party
wallpaper and personal data in the frame) and tablet screenshots (the app is not optimised for
tablets).

Rebuild and check:

```sh
python3 scripts/build-play-store-assets.py      # from play-store/source/raw screenshots
python3 scripts/validate-play-store-assets.py   # dimensions, formats, sizes, alpha, aspect ratio
```

`source/raw/` holds the unedited captures; `source/fonts/Roboto.ttf` is Roboto (Apache License
2.0), used for the headings.

## Upload checklist

1. **App bundle:** upload `rcb-wake-1.0.0-release.aab` from `~/development/rcb-wake-releases/1.0.0/`.
   It is signed with the RCB Wake upload key (`~/development/rcb-wake-signing/`); enrol in Play
   App Signing. Keep the keystore and its passwords backed up — losing them blocks updates.
2. **Store listing:** copy the texts from `store-listing.md`; upload the graphics above.
3. **Privacy policy URL:** https://github.com/Rananjaya/RCB_Wake/blob/main/PRIVACY_POLICY.md
   (the same text is in the app under Settings → Privacy policy).
4. **App content:** answers below.
5. **Release:** start with an internal or closed test track, then production.

## App content answers

**Ads:** No ads. **In-app purchases:** None. **Target audience:** 13 and older (general
utility; not designed for children). **Content rating questionnaire:** Utility; no violence,
sexual content, gambling, user-generated content sharing, or unrestricted internet browsing.
**News app:** No. **Government app:** No. **Financial features:** None. **Health:** No.

### Data safety

RCB Wake has no server and no analytics. Data leaves the device only through optional features the
user turns on, sent directly from the phone to the service. Suggested answers (review before
submitting — you are responsible for the final declaration):

| Data type | Collected? | Shared? | Notes |
| --- | --- | --- | --- |
| Location → Approximate location | Yes, optional | No | Rounded coordinates sent to Open-Meteo, MET Norway or OpenWeatherMap for widget weather. Processed ephemerally; purpose: App functionality. |
| App activity → In-app search history | Yes, optional | No | Place-search text sent to Open-Meteo geocoding. Ephemeral; App functionality. |
| Personal info, financial, health, messages, photos/videos, audio, files, calendar, contacts, device IDs | No | No | Calendar events and camera frames are processed on the device only. |

- Data encrypted in transit: **Yes** (HTTPS only).
- Users can request deletion: data is stored only on the device; uninstalling or clearing app
  storage deletes it.
- If you list OpenWeatherMap support explicitly, note that the user's own API key is sent to
  OpenWeather with each request.

### Permission declarations

| Permission | Declaration |
| --- | --- |
| `USE_EXACT_ALARM` | Core functionality: the app is an alarm clock; alarms must ring at the exact minute. |
| `USE_FULL_SCREEN_INTENT` | Alarm clock: shows the ringing alarm over the lock screen. |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Plays the alarm sound until the user snoozes or dismisses it. Play may ask for a short video of an alarm ringing. |
| `ACCESS_COARSE_LOCATION` | Optional, foreground only: weather for the widget. No background location. |
| `CAMERA` | Optional: QR-code wake-up challenge, used only while the scanner is open. |
| `READ_CALENDAR` | Optional: next event on the home-screen widget; read on device only. |

## Before production — still to verify

- Scan a real QR code with the QR challenge on a phone (implemented and unit-tested; the camera
  scan itself has not yet been tested on a device).
- Test with an OpenWeatherMap API key.
- Overnight alarm test on a Samsung and a Xiaomi phone with battery optimisation on.
