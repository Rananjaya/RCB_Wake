# Third-Party Notices

RCB Wake's own source code is licensed under the PolyForm Noncommercial License 1.0.0 (see
[LICENSE](LICENSE)). It includes or depends on the third-party software and assets below, which
remain under their own licences. Nothing here changes those licences.

## Bundled in the app

| Component | Version | Licence | Source |
| --- | --- | --- | --- |
| React | 19.3.0 | MIT | https://github.com/react/react |
| React Native (incl. Hermes) | 0.88.1 | MIT | https://github.com/react/react-native |
| React Navigation (native, bottom-tabs, native-stack) | 7.x | MIT | https://github.com/react-navigation/react-navigation |
| react-native-screens | 4.30.1 | MIT | https://github.com/software-mansion/react-native-screens |
| react-native-safe-area-context | 5.11.1 | MIT | https://github.com/appandflow/react-native-safe-area-context |
| react-native-gesture-handler | 3.5.0 | MIT | https://github.com/software-mansion/react-native-gesture-handler |
| react-native-reanimated | 4.8.0 | MIT | https://github.com/software-mansion/react-native-reanimated |
| react-native-worklets | 0.14.0 | MIT | https://github.com/software-mansion/react-native-reanimated |
| react-native-svg | 15.16.1 | MIT | https://github.com/software-mansion/react-native-svg |
| zustand | 5.1.0 | MIT | https://github.com/pmndrs/zustand |
| AndroidX libraries (via React Native) | — | Apache License 2.0 | https://developer.android.com/jetpack/androidx |
| Kotlin standard library | 2.2 | Apache License 2.0 | https://github.com/JetBrains/kotlin |
| Material Symbols: `alarm` (launcher icon glyph), `settings` (settings gear) | 0.49.0 | Apache License 2.0 | https://github.com/google/material-design-icons — full text in [licenses/material-symbols-LICENSE.txt](licenses/material-symbols-LICENSE.txt) |

Each npm package's full licence text ships in its `node_modules/<package>/LICENSE` file and is
reproduced in builds that include that package.

## Data services

| Service | Use | Terms |
| --- | --- | --- |
| [Open-Meteo](https://open-meteo.com) | Optional widget weather (off by default) | Weather data licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Attribution is shown in Settings → Widget weather. The free API is intended for non-commercial use; a commercial distribution of RCB Wake would need an Open-Meteo commercial plan. |

## Original assets

These were created for RCB Wake and are covered by the project licence:

- Alarm tones in `android/app/src/main/res/raw/` — synthesised by `scripts/generate-tones.py`.
- Weather icons `ic_wx_*` — generated from primitives by `scripts/generate-weather-icons.py`.
- UI line icons in `src/components/Icon.tsx` (except the settings gear noted above), the
  floral accent, the ringing screen and the widget layouts.

## Development-only tools

Babel, ESLint, Prettier, Jest, TypeScript, Metro, Gradle and the Android Gradle Plugin are used to
build and test RCB Wake but are not shipped inside the app. Their licences are in
`node_modules/` and in the Gradle distribution.
