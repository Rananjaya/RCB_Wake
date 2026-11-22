/**
 * RCB Wake privacy policy — the single source for the in-app screen and PRIVACY_POLICY.md
 * (regenerate the Markdown with `node scripts/export-privacy-policy.js`; a test checks they match).
 * Keep it factual: every statement must match what the app actually does.
 */
export type PolicySection = { title: string; paragraphs: string[] };

export const PRIVACY_POLICY_EFFECTIVE = '22 November 2026';

export const PRIVACY_POLICY_INTRO = [
  'RCB Wake ("the app") is an alarm clock for Android with timers, a Pomodoro timer, a stopwatch, a world clock, a bedtime reminder and a home-screen clock widget. It is developed by Rananjaya ("we"). This policy explains what information the app handles, where it goes and the choices you have.',
  'In short: RCB Wake has no accounts, advertising, analytics, crash reporting or tracking. Everything you create in the app stays on your phone. The app only uses the internet for optional features that are off until you turn them on.',
];

export const PRIVACY_POLICY_SECTIONS: PolicySection[] = [
  {
    title: 'Information stored on your device',
    paragraphs: [
      'Alarms (times, repeat days, labels, sound choice and options), timers, Pomodoro settings and progress, stopwatch laps, world-clock cities, your settings, and the bedtime-reminder preferences are stored only in the app’s private storage on your phone.',
      'If you choose a custom alarm sound, the app stores a reference to the file you picked and reads it in place; the file is not copied or uploaded.',
      'This data is excluded from Android cloud backup and device-to-device transfer, so it never leaves the phone through backups. It is deleted when you uninstall the app or clear its storage.',
    ],
  },
  {
    title: 'What we do not collect',
    paragraphs: [
      'We do not collect, receive or store any personal information on our own servers — the app has no server. There are no user accounts, no advertising or advertising identifiers, no analytics, no crash reporting, no tracking, and no selling or sharing of data.',
    ],
  },
  {
    title: 'Optional widget weather (off by default)',
    paragraphs: [
      'If you turn on weather for the home-screen widget, the app requests the current weather for one location at the interval you choose (15 minutes to 3 hours) while a widget is on your home screen, and when you change the location.',
      'Open-Meteo (open-meteo.com) is the default provider. The app sends only the latitude and longitude of the chosen location, rounded to two decimal places (about 1 km). No identifier or key is sent.',
      'If you choose MET Norway (api.met.no) instead, the app sends the same rounded coordinates and identifies itself as RCB Wake (app name, version and project website, as MET Norway requires); no key or personal data is sent.',
      'If you choose OpenWeatherMap (openweathermap.org) instead, the app sends the same rounded coordinates together with the API key you entered. Your key is stored only on your phone and is sent only to OpenWeatherMap; OpenWeather may associate requests with your OpenWeather account under its own privacy policy.',
      'Like any web service, the weather provider can see your device’s IP address. The latest weather reading is cached on the phone. Turning weather off stops all weather requests and deletes the cached reading.',
    ],
  },
  {
    title: 'Optional place search',
    paragraphs: [
      'When you search for a city, village or suburb as the weather location, the text you type (from two characters) is sent to Open-Meteo’s geocoding service to find matching places. Place data is from GeoNames. Nothing else is sent, and searches are not stored by the app.',
    ],
  },
  {
    title: 'Optional location',
    paragraphs: [
      'If you turn on “Use my location” for weather and grant approximate-location permission, the app reads your approximate location only while it is open. It stores the position rounded to about 1 km and uses it for weather requests. The app never requests precise or background location.',
      'To show the name of your area (for example a village or suburb), the app uses Android’s built-in geocoder when the position changes. On phones with Google services, Android sends the approximate position to Google’s geocoding service for this; only the resulting place name is stored.',
      'Turning the option off deletes the stored position and place name.',
    ],
  },
  {
    title: 'Optional calendar on the widget',
    paragraphs: [
      'If you choose to show your next event on the widget and grant calendar permission, the app reads upcoming events from the calendars you select on your phone (for example Google Calendar or RCB Calendar). Event details are only displayed on the widget; they are never stored by the app or sent anywhere.',
    ],
  },
  {
    title: 'Optional earphones on the widget',
    paragraphs: [
      'If you turn on “Show connected earphones”, the My Day widget shows the name of connected wired or Bluetooth earphones and, for Bluetooth earphones, their battery level. With the Nearby devices permission the app reads this from Android on your phone; it is only displayed and is never stored or sent anywhere.',
    ],
  },
  {
    title: 'Optional QR-code wake-up challenge',
    paragraphs: [
      'If you choose the QR-code challenge for an alarm and grant camera permission, the camera is used only while the scanner is open during that alarm. Images are processed on the phone to detect a QR code; no photo or video is saved or sent, and the content of the scanned code is not kept.',
    ],
  },
  {
    title: 'Permissions',
    paragraphs: [
      'Exact alarms, notifications, full-screen notifications, run at startup, foreground service (media playback), vibration and wake lock are used so that alarms, timers and reminders ring on time, over the lock screen and after a restart.',
      'Internet (optional weather and place search), approximate location (optional), calendar (optional), camera (optional QR challenge) and nearby devices (optional earphones on the widget) are requested only when you turn on the feature that needs them. You can withdraw any permission at any time in Android settings.',
    ],
  },
  {
    title: 'Children',
    paragraphs: [
      'RCB Wake is a general-audience utility and is not directed at children under 13. It does not knowingly collect personal information from anyone, including children.',
    ],
  },
  {
    title: 'Security',
    paragraphs: [
      'All network requests use encrypted HTTPS connections. Data stays in the app’s private storage, which other apps cannot read.',
    ],
  },
  {
    title: 'Your choices and data deletion',
    paragraphs: [
      'You can turn every optional feature off in Settings, revoke permissions in Android settings, and delete all app data by clearing the app’s storage or uninstalling it. Because we hold no data about you, there is nothing for us to delete on a server.',
    ],
  },
  {
    title: 'Third-party services',
    paragraphs: [
      'The optional features above use: Open-Meteo (weather and place search; open-meteo.com/en/terms), MET Norway (weather; api.met.no/doc/TermsOfService), OpenWeather (weather, only with your own key; openweather.co.uk/privacy-policy) and, for place names on phones with Google services, Google’s geocoding through Android (policies.google.com/privacy). Their own privacy policies apply to the requests they receive.',
    ],
  },
  {
    title: 'Changes to this policy',
    paragraphs: [
      'If the app’s handling of information changes, this policy will be updated and the effective date changed. Significant changes are shown in the app before you continue using it.',
    ],
  },
  {
    title: 'Contact',
    paragraphs: [
      'Questions about this policy: open an issue at github.com/Rananjaya/RCB_Wake/issues.',
    ],
  },
];

/** Markdown version, used for PRIVACY_POLICY.md (the public URL for the Play Store listing). */
export function privacyPolicyMarkdown(): string {
  const lines = [
    '# RCB Wake Privacy Policy',
    '',
    `Effective date: ${PRIVACY_POLICY_EFFECTIVE}`,
    '',
    ...PRIVACY_POLICY_INTRO.flatMap(p => [p, '']),
  ];
  for (const s of PRIVACY_POLICY_SECTIONS) {
    lines.push(`## ${s.title}`, '');
    for (const p of s.paragraphs) lines.push(p, '');
  }
  lines.push(
    '<!-- Generated from src/legal/privacyPolicy.ts by scripts/export-privacy-policy.js. Do not edit by hand. -->',
    '',
  );
  return lines.join('\n');
}
