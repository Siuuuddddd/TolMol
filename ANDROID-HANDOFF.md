# TolMol Android handoff

The Android app lives in `android/` and uses the backend in `backend/`. The root README covers setup, endpoint settings, emulator location, and build commands.

- The Compose app uses Koin, type-safe Navigation 3, Retrofit, DataStore JWT sessions, and a shared OkHttp WebSocket. Separate buyer and seller tabs use feature-scoped presentation, domain, and data packages. The Android unit tests verify the Koin module.
- Included flows are registration and sign-in, role-specific device location, nearby discovery and watchlists, live auctions, bid history, seller inventory and award, private chat, read/unread alerts, pickup inspection, settlement, and dispute cancellation and relisting.
- Use two accounts and nearby emulator locations to exercise one sale. With a different measured weight, exercise both revision confirmation and cancellation on separate deals.

Verified on the Pixel 9 Pro emulator against the local backend. Sign-in and role selection loaded nearby inventory, and a competing bid sent to the backend updated the open auction over the shared WebSocket.

Background push, maps, uploads, payment processing, offline storage, and localization are outside the Android MVP. Production requires a hosted backend with TLS. FCM can be integrated later using the backend's existing device-registration API.

## Google sign-in — current contract

- Android uses Credential Manager with GOOGLE_WEB_CLIENT_ID from ignored android/local.properties. The auth feature's dedicated GoogleSignInUseCase calls POST /auth/google with idToken, loads the profile and stores the returned TolMol JWT through the normal DataStore session path. Success opens existing role selection/home. Both account forms and the welcome action are wired.
- Missing configuration displays "Google sign-in is not configured yet" without breaking email login or registration. Cancellation shows no error; no-account, connection and rejected-token failures have readable retry messages. Facebook UI, SDK dependency and callback bridge are removed.
- Backend V5 supports verified, passwordless Google accounts and a unique subject. Verified email can link an existing account once, preserving its password/profile/roles. Google-only accounts cannot use password login. The new endpoint is Redis-rate-limited.
- One manual setup remains: create the owner's Google Web and Android OAuth clients, register package/debug SHA-1, put the same Web client ID in android/local.properties (GOOGLE_WEB_CLIENT_ID) and backend/.env (GOOGLE_CLIENT_ID), and use a Google Play emulator with an account signed in. README's GOOGLE SIGN-IN section contains the exact steps. No Facebook configuration, Firebase or secrets are needed. Both local ID settings are currently absent; live provider acceptance cannot be performed until this setup is supplied.
- Verification: full backend build and 31 tests passed, including signed-RSA wrong-audience/signature/issuer/expiry tests, Google account creation/linking and the IP limit. Android assembly and all 5 JVM tests passed, including Google failure/retry/role state and Koin graph verification.
