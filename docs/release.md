# Building and releasing

## Requirements

Node.js ≥ 22.11, Yarn 1.x, JDK 17, Android SDK with platform 37 and build tools 37.0.0,
NDK 27.1.12297006 (used by React Native native modules).

## Debug build

```sh
yarn install
cd android && ./gradlew assembleDebug
# android/app/build/outputs/apk/debug/app-debug.apk
```

## Release signing

Release builds are signed only when a signing properties file is provided. Keep the keystore and
the properties file **outside** the repository.

1. Create a keystore once:
   ```sh
   keytool -genkeypair -v -keystore rcb-wake-release.jks -alias rcbwake \
     -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Create `rcb-wake-signing.properties` next to it:
   ```properties
   storeFile=/absolute/path/to/rcb-wake-release.jks
   storePassword=…
   keyAlias=rcbwake
   keyPassword=…
   ```
3. Build with the path in an environment variable:
   ```sh
   export RCB_WAKE_SIGNING_PROPERTIES=/absolute/path/to/rcb-wake-signing.properties
   cd android
   ./gradlew assembleRelease   # APK: app/build/outputs/apk/release/app-release.apk
   ./gradlew bundleRelease     # AAB: app/build/outputs/bundle/release/app-release.aab
   ```

Without the variable, `assembleRelease` produces an unsigned APK.

Back up the keystore and passwords securely: losing them means you can no longer publish updates
under the same app identity.

## Release checks

```sh
yarn typecheck && yarn lint && yarn test && yarn test:kotlin
# Permissions in the merged release manifest
grep uses-permission android/app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml
# Signature
$ANDROID_HOME/build-tools/37.0.0/apksigner verify --print-certs android/app/build/outputs/apk/release/app-release.apk
```

Expected release permissions are listed in [security-privacy.md](security-privacy.md).

## Versioning

`versionName` and `versionCode` live in `android/app/build.gradle` (1.0.0 / 100). Record changes in
[CHANGELOG.md](../CHANGELOG.md).

## Google Play notes

- Declare `USE_EXACT_ALARM` under the alarm-clock exception in the Play Console permissions
  declaration.
- Declare the `mediaPlayback` foreground service type and its use (alarm audio).
- Data safety: no data collected; the optional weather request sends approximate location to
  Open-Meteo — declare "Approximate location, not collected/shared for tracking, optional".
