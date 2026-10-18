# MOBILITY Android

Android companion app for the existing MOBILITY marketplace website.

## Source of truth

The existing marketplace remains in [`Syed-Haseeb97/mobility_03`](https://github.com/Syed-Haseeb97/mobility_03) and is served at `https://mobility-03.vercel.app`. This repository adds Android-specific capabilities without replacing the existing web product with a second, divergent marketplace implementation.

## Current direction

- Android shell that loads the existing responsive website, preserving the current renter, vehicle owner, and admin screens.
- Native Android location permission and foreground trip tracking for explicitly started rental trips.
- Trip metrics: GPS speed, distance, elapsed time, average speed, and top speed; filter low-accuracy fixes and implausible jumps.
- Tracking must be visible to the renter and only run during an authorized, explicitly started rental. Live admin visibility requires a secure server-side trip API and real authentication; it must not be faked with local-only data.

## Build

Open this repository in Android Studio and sync Gradle. Use a current stable Android SDK and JDK 17.

> The initial Android implementation is a development foundation. The website currently documents demo/persistence and authentication limitations; do not treat a demo booking as a real paid rental or the Android app as production-ready until server-side identity, trip authorization, database persistence, and live updates are integrated and tested.

## In-app Android updates

The native **History** screen includes **Check for app updates**. It checks the public GitHub release tagged `android-latest`, downloads `MOBILITY-latest.apk`, and opens Android's package installer.

To enable publishing, configure a **persistent signing key** in GitHub repository **Settings → Secrets and variables → Actions**. Create a keystore once and keep it backed up securely; never rotate it for routine updates.

On Windows with Android Studio's JDK available, create the keystore locally:

```powershell
keytool -genkeypair -v -keystore mobility-release.jks -alias mobility -keyalg RSA -keysize 2048 -validity 10000
$env:ANDROID_SIGNING_KEY_BASE64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("$PWD\mobility-release.jks"))
```

Add these repository Actions secrets:
- `ANDROID_SIGNING_KEY_BASE64`: the value of `$env:ANDROID_SIGNING_KEY_BASE64`
- `ANDROID_KEY_ALIAS`: `mobility`
- `ANDROID_KEYSTORE_PASSWORD`: the keystore password chosen above
- `ANDROID_KEY_PASSWORD`: the key password chosen above

Do not commit the keystore or paste its contents into source code. Once the secrets exist, each successful push to `main` publishes a signed APK release and the in-app updater can discover it. The key must remain the same for all future releases; changing it prevents Android from updating an already-installed copy. If the app was installed from an earlier CI debug APK, uninstall that copy before installing the first stable-key release (this clears local app data).
