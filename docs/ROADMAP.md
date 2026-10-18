# Android integration roadmap

## Included in the foundation
- Uses the deployed MOBILITY site inside Android WebView, preserving existing renter, host, and admin screens rather than creating a parallel marketplace.
- Native Explore / My trip / Trip history navigation.
- Explicit-consent trip start, visible foreground location notification, local GPS distance and speed estimates, accuracy/jump filtering, and stop control.
- Trip summary survives app restarts on the same device.

## Required before production
1. Add real authentication and server-side booking authorization; verify the renter owns the booking and that the rental is active before accepting a trip.
2. Add secure trip APIs to `mobility_03` for session creation, validated location batches, trip end, and role-protected summaries.
3. Replace the website's prototype JSON storage with a transactional managed database.
4. Add authenticated live admin updates; never expose a renter's location to other renters.
5. Define retention and test background/Doze, permission denial, poor GPS, force-stop, reboot, and battery impact on real devices.
6. Add Gradle wrapper, Android build CI, signing configuration, and release tests.

## Metric definitions
- Distance: sum of accepted GPS segments after accuracy and jump filtering.
- Current/top speed: GPS-reported speed in m/s converted to km/h when available.
- Average speed: accepted distance divided by accumulated moving time (segments at or above 0.8 m/s).
- These are GPS estimates, not a certified odometer.
