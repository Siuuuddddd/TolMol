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

## Shared UI and MVP completion

- Every app text style inherits the existing bundled Pixelify Sans font from core/ui/Typography.kt. No screen owns a font family or light theme. Native welcome text, form fields/helper text, list values, dialog text, chips and bottom-navigation labels use it. All filled/outlined actions share pixel shapes and the dark factory/lime palette.
- The welcome layout and graphics remain, with native scalable lettering over a textless original-style asset. Account forms keep their layout and behavior but use the same dark background and factory header as the welcome screen. No cream/cobalt theme remains in screen code.
- All original Android MVP milestones are implemented and audited through navigation and loading/empty/error/retry rendering. No Hilt references remain in Android source or dependency configuration. No additional frontend implementation milestone is pending.
- Current validation: assembleDebug plus all 5 Android unit tests pass (including Koin and Google retry/role state); the complete backend build and all 31 tests pass. On Pixel 9 Pro the friendly missing-ID behavior was checked on welcome and registration, Facebook absence was checked, password login reached role selection/discovery against the updated backend, and enlarged 1.3 font scaling kept native welcome text and Google reachable. The local backend is running with V5 applied and /health UP. Real Google account acceptance remains the one manual OAuth/client-ID setup described above.
