# TolMol Android handoff

The Android app lives in `android/` and uses the backend in `backend/`. The root README covers setup, endpoint settings, emulator location, and build commands.

- The Compose app uses Koin, type-safe Navigation 3, Retrofit, DataStore JWT sessions, and a shared OkHttp WebSocket. Separate buyer and seller tabs use feature-scoped presentation, domain, and data packages. The Android unit tests verify the Koin module.
- Included flows are registration and sign-in, role-specific device location, nearby discovery and watchlists, live auctions, bid history, seller inventory and award, private chat, read/unread alerts, pickup inspection, settlement, and dispute cancellation and relisting.
- Use two accounts and nearby emulator locations to exercise one sale. With a different measured weight, exercise both revision confirmation and cancellation on separate deals.

Verified on the Pixel 9 Pro emulator against the local backend. Sign-in and role selection loaded nearby inventory, and a competing bid sent to the backend updated the open auction over the shared WebSocket.

Background push, maps, uploads, payment processing, offline storage, and localization are outside the Android MVP. Production requires a hosted backend with TLS. FCM can be integrated later using the backend's existing device-registration API.

## Authentication screen update — 2026-11-25

- Welcome now uses compact, non-clickable Sell scrap/Buy scrap nearby text cards, native Pixelify Sans headings, Sign in/Create an account actions, and Google/Facebook buttons. The primary Sign in action invokes Google account authentication.
- Account creation uses original cobalt pixel mailbox artwork over a cream form with name, email, password, visibility control, validation and social options. Role chips are removed; new accounts have both roles. The form's Sign in link retains email/password access.
- Backend adds optional registration name, V4 profile/identity migration and verified provider-token exchange. Existing accounts are not auto-linked by email. README Android section contains exact configuration names and setup references.
- Blocked item: TolMol Google OAuth Web client ID plus Android package/SHA registration, and Facebook app ID/client token/server app secret plus package/key-hash registration. Add the values locally as documented; never commit or send the Facebook app secret in chat. Real provider-account acceptance cannot be confirmed until these are supplied.

Validation: Android assembleDebug and all 5 JVM tests passed, including Koin verification; the full backend build passed all 22 tests. Pixel 9 Pro checks covered the new screens, real local email registration/login with both roles, keyboard entry, 1.3 font scaling on sign-in, and missing-provider error handling. The updated local backend runs through backend/gradlew bootRun; the stale Docker app container was stopped to free port 8080.

Welcome design correction: restored the original factory artwork and metal card graphics rather than the cropped conveyor banner and flat panels. Cards are narrower, informational and non-clickable. The one-account note is removed. Sign in still invokes Google, Create an account opens the existing mailbox form, and Google/Facebook options remain below. Android assembly and all 5 JVM tests pass; no backend code changed in this correction.
