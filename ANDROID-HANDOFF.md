# TolMol Android handoff

The Android app lives in `android/` and uses the backend in `backend/`. The root README covers setup, endpoint settings, emulator location, and build commands.

- The Compose app uses Koin, type-safe Navigation 3, Retrofit, DataStore JWT sessions, and a shared OkHttp WebSocket. Separate buyer and seller tabs use feature-scoped presentation, domain, and data packages. The Android unit tests verify the Koin module.
- Included flows are registration and sign-in, role-specific device location, nearby discovery and watchlists, live auctions, bid history, seller inventory and award, private chat, read/unread alerts, pickup inspection, settlement, and dispute cancellation and relisting.
- Use two accounts and nearby emulator locations to exercise one sale. With a different measured weight, exercise both revision confirmation and cancellation on separate deals.

Verified on the Pixel 9 Pro emulator against the local backend. Sign-in and role selection loaded nearby inventory, and a competing bid sent to the backend updated the open auction over the shared WebSocket.

Background push, maps, uploads, payment processing, offline storage, and localization are outside the Android MVP. Production requires a hosted backend with TLS. FCM can be integrated later using the backend's existing device-registration API.
