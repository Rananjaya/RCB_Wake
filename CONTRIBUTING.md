# Contributing to RCB Wake

Thanks for your interest in improving RCB Wake. Bug reports, translations and focused pull
requests are all welcome.

## Before you start

- RCB Wake is **source-available** under the PolyForm Noncommercial License 1.0.0. By submitting
  a contribution you agree that it is provided under the same licence, and that the maintainer may
  also distribute it as part of RCB Wake under separate commercial terms (see
  [COMMERCIAL_LICENSE.md](COMMERCIAL_LICENSE.md)).
- Please read and follow the [Code of Conduct](CODE_OF_CONDUCT.md).
- For anything larger than a small fix, open an issue first so we can agree on the approach.
- Report security issues privately — see [SECURITY.md](SECURITY.md).

## Development setup

Requirements: Node.js ≥ 22.11, Yarn 1.x, JDK 17, Android SDK (API 37 platform, build tools 37),
and an emulator or device running Android 8.0 (API 26) or newer.

```sh
yarn install
yarn start          # Metro bundler
yarn android        # build, install and launch the debug app
```

On an emulator that cannot reach your machine at `10.0.2.2`, run `adb reverse tcp:8081 tcp:8081`
and set the dev server host to `localhost:8081` from the React Native dev menu.

## Checks

Run all of these before opening a pull request:

```sh
yarn typecheck      # TypeScript
yarn lint           # ESLint + Prettier rules
yarn test           # Jest (JS logic and stores)
yarn test:kotlin    # JVM unit tests for the native engine
```

Changes to scheduling, ringing, timers or receivers must also be checked on a device or emulator
using the manual scenarios in [docs/testing.md](docs/testing.md).

## Guidelines

- **Alarm reliability comes first.** Never move alarm, timer or Pomodoro timing into JavaScript;
  the Kotlin engine must work without React Native running.
- **Privacy.** Do not add network access, analytics, tracking or new permissions without an issue
  discussing it first.
- **Strings.** Every user-facing string goes through `src/i18n` (JS) or `res/values*/strings.xml`
  (native) in all supported languages. The i18n test fails if a language is missing a key.
- **Style.** Prettier for TypeScript, the standard Kotlin style for Kotlin. Keep functions small
  and comments about *why*, not *what*.
- **Commits.** Use [Conventional Commits](https://www.conventionalcommits.org) (`feat:`, `fix:`,
  `docs:`, `test:`, `refactor:`, `chore:` …) with short, descriptive messages.
- **Docs.** Update the relevant file in `docs/` in the same pull request as the code.

## Translations

Translations live in `src/i18n/<language>.ts` and
`android/app/src/main/res/values-<language>/strings.xml`. Improvements from native speakers are
especially welcome.
