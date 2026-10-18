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
