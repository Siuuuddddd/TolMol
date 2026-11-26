# TolMol

TolMol is a Kotlin and Spring Boot modular monolith for local scrap auctions. It uses PostgreSQL for identity, locations, marketplace and chat, Redis for caches, Kafka for events, and one authenticated WebSocket for live updates.

## Run locally

Install JDK 21 and start Docker Desktop with Linux containers. From the `backend/` folder in PowerShell, run the following once to create local development credentials. The application and Compose both read the ignored `.env` file.

```powershell
if (!(Test-Path .env)) {
    @(
        "TOLMOL_DB_PASSWORD=$([guid]::NewGuid().ToString('N'))"
        "TOLMOL_JWT_SECRET=$([guid]::NewGuid().ToString('N'))$([guid]::NewGuid().ToString('N'))"
    ) | Set-Content -Encoding ascii .env
}
docker compose up -d
.\gradlew.bat bootRun
```

The API listens on `http://localhost:8080`. Check `GET /health`. Flyway migrates the four databases on startup. The development host ports are 15432 for PostgreSQL, 16379 for Redis and 9092 for Kafka. Keep the database password used to initialize the Compose volume. Use `docker compose stop` to stop infrastructure while retaining data.

Run `.\gradlew.bat build` for the full build and integration tests. Tests use their own disposable PostgreSQL, Redis and Kafka containers. On macOS or Linux, create the same two variables in a local `.env` file and use `./gradlew`.

## Android

For Windows IDE imports, run `.\setup-workspace.ps1` from the repository root before opening the projects. This restores the backend Gradle link and saves each IDE's local Java home. Reopen an already open project after running the script. Optional `-BackendJavaHome` and `-AndroidJavaHome` arguments support other installation paths.

1. Start the backend from `backend/` as above. Check that `http://localhost:8080/health` reports `UP`.
2. Open `android/` in Android Studio and sync with its bundled JDK. For command-line builds use JDK 21. Install Android SDK 37 from SDK Manager if prompted. Run the **app** configuration on an emulator with Google Play services. The reference device is Pixel 9 Pro.
3. In the emulator's **Extended controls → Location**, set and send a point. Sign in and allow location. Accounts with buyer access open nearby discovery directly; use **Switch to selling** on home to open your listings. Seller-only accounts open listings directly. A buyer and a seller must be separate accounts to test bidding on the seller's lot.
4. Emulator debug builds resolve the host at `10.0.2.2:8080`. For a physical device on the same network, add the following to your user Gradle properties (`~/.gradle/gradle.properties`), using your computer's reachable LAN IP, then sync and rebuild.

   ```properties
   tolmolDebugRestUrl=http://192.168.1.20:8080/
   tolmolDebugSocketUrl=ws://192.168.1.20:8080/ws
   ```

   On Android 17, grant the debug app local-network permission when prompted. Release builds require HTTPS and WSS. Set `tolmolReleaseRestUrl` and `tolmolReleaseSocketUrl` to your hosted backend.

If an earlier import reports **Invalid Gradle JDK configuration**, select **Use Embedded JDK** in the sync result and sync again.

From `android/`, run `./gradlew assembleDebug testDebugUnitTest` or `.\gradlew.bat assembleDebug testDebugUnitTest` on Windows. The unit suite includes Koin module verification.

The UI uses the locally bundled, SIL OFL licensed **Pixelify Sans** for every Material text style and all custom text. One typography definition in `core/ui/Typography.kt` controls headings, forms, lists, numbers, dialogs, chips, snackbars and navigation labels. All screens inherit the dark factory palette, lime actions and shared cut-corner button shapes. Welcome artwork is textless so lettering remains native and responds to Android font scaling. No font download is needed.

### GOOGLE SIGN-IN

Google sign-in uses Android Credential Manager and the backend's `POST /auth/google` endpoint. The device sends `{ "idToken": "..." }`; the backend verifies Google's signature, issuer, expiry, audience and verified email, then returns the same `{ "accessToken": "..." }` response as password login. The app stores that TolMol JWT in DataStore and opens the home screen directly. No Firebase setup is needed.

1. In [Google Cloud Console](https://console.cloud.google.com/apis/credentials), configure the OAuth consent screen and create **both** a Web OAuth client and an Android OAuth client in the same project. Use package `com.tolmol.app` for Android and register the debug signing certificate's SHA-1. If the consent screen is in testing mode, add your test Google account.
2. From `android/`, run `./gradlew signingReport` (Windows: `.\gradlew.bat signingReport`) and copy the debug **SHA-1** to the Android OAuth client. Keep the same debug keystore when rebuilding; register the new fingerprint if you change it.
3. Put the **Web** client ID, not the Android client ID, in the ignored `android/local.properties`. Keep its existing `sdk.dir` entry:

   ```properties
   GOOGLE_WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
   ```

4. Put that **same Web client ID** in the ignored `backend/.env`:

   ```properties
   GOOGLE_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
   ```

5. Restart the backend, sync Android Studio and rebuild the app. Use a Pixel 9 Pro emulator image marked **Google Play**, with a Google account signed into the emulator. Sign in and the Google button on both account forms use this flow. An authorized account may be selected automatically; otherwise Google displays account selection/consent. Cancellation shows no error. Facebook is hidden.

A missing client ID leaves Google visible and displays "Google sign-in is not configured yet"; email/password and the rest of the app still work. No OAuth ID, token or secret is committed. Real Google acceptance requires the owner's provider setup and client ID; all code and automated tests can run without it.

If Google sign-in worked before but fails after restarting Docker Desktop, check which backend owns port 8080. An existing app container keeps its old image and environment even when the source or `.env` changes. From `backend/`, refresh the container backend with:

```powershell
.\gradlew.bat bootJar
docker compose --profile app up --build --force-recreate -d app
```

Wait for `http://localhost:8080/health` to report `UP`, then retry Google sign-in. This rebuilds the application image and reloads `.env`. For host `bootRun` development, stop the container backend first with `docker compose stop app` so both servers do not compete for port 8080. Restarting an old container alone does not reload OAuth configuration.

Verified Google email links an existing email account once, preserving its password, profile and roles. New Google accounts have no password hash, verified email and the Android registration defaults (BUYER and SELLER). A subject already linked to another account cannot replace the existing link. Future sign-ins use the stable Google subject. The new endpoint allows 30 attempts per peer IP per minute using Redis; arbitrary forwarded headers cannot bypass the limit. Existing password registration/login remain unchanged.

Implementation references: [Android Credential Manager](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation), [Google server verification](https://developers.google.com/identity/gsi/web/guides/verify-google-id-token).

## REST API

Send JSON bodies and use `Authorization: Bearer <accessToken>` on authenticated requests. Currency fields are decimal rupees, rates are rupees per kilogram, weights are kilograms, deadlines are ISO-8601 instants, and IDs are UUIDs except the numeric chat message cursor.

| Endpoint | Purpose |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | Register with email, password and roles such as `["SELLER","BUYER"]`. Login with email and password for a token. |
| `POST /auth/google` | Verify a Google `idToken`, link or create the account, and return a TolMol JWT. Registration also accepts an optional `name`. |
| `GET /api/me` | Current account ID and available roles. |
| `PUT /api/me/location/{BUYER or SELLER}` | Set latitude and longitude for that role. Refresh within 15 minutes before publishing, browsing or bidding. |
| `POST /api/listings` | Publish with category, weight, startingRate and deadline. Uses the seller’s stored location. |
| `GET /api/listings`, `GET /api/listings/watchlist` | Nearby open lots and nearby watched lots. |
| `PUT` or `DELETE /api/listings/{id}/watch` | Watch or unwatch a lot. |
| `POST /api/listings/{id}/bids` | Submit `rate` and a UUID `requestKey`. Reuse the key when retrying. |
| `GET /api/listings/{id}/auction`, `GET /api/listings/{id}/bids` | Read the auction snapshot or, as seller, the list of offers. |
| `POST /api/listings/{id}/accept` | Seller awards a recorded `bidId`. At the deadline the backend awards the highest bid automatically, or expires an empty auction. |
| `POST /api/listings/{id}/chat` | Buyer opens or retrieves their thread with the seller. Returns the thread ID. |
| `GET /api/chat/{threadId}` | Participant-only thread details with current writable state. |
| `GET /api/chat/{threadId}/messages` | Latest 50 messages. Supply `?after=0` to start from the oldest, then pass the last message ID for the next page. |
| `GET /api/notifications` | Current user’s alerts, newest first, with cursor pagination and read state. |
| `GET /api/deals`, `GET /api/deals/{listingId}` | Participant-only deal details including pickup coordinates and current revision. |
| `POST /api/deals/{listingId}/inspect` | Winning buyer submits measured `weight`. A match calculates the total. A mismatch opens a dispute. |
| `POST /api/deals/{listingId}/propose` | Either participant proposes an `amount` during a dispute. |
| `POST /api/deals/{listingId}/confirm` | The other participant confirms the current `revision` after settlement. With matching weight the seller confirms. |
| `POST /api/deals/{listingId}/cancel` | Either participant ends a weight dispute and relists with the measured weight. The response includes the new listing ID. |

## WebSocket

Connect to `ws://localhost:8080/ws` with the bearer header on the HTTP upgrade. Use the same connection for messages, auction subscriptions and alerts. Replace the placeholders with actual UUIDs.

```json
{"type":"auction.subscribe","listingId":"<listing UUID>"}
```

```json
{"type":"chat.send","threadId":"<thread UUID>","clientId":"<new UUID>","body":"Can I collect today?"}
```

Commands return `ack` with data or `error` with a status. Incoming events contain `eventId`, `type`, `aggregate` and `data`. Deduplicate by event ID. After reconnecting, resubscribe and fetch the latest auction snapshot, chat history or alert inbox. Offline push defaults to logging and supports configured FCM/APNs delivery. Payment confirmation records an agreement between participants and does not transfer funds.
Personal history: GET /api/me/listings, /api/me/bids and /api/me/conversations. Pass limit (1–100, default 25) and after (last returned UUID) until an empty page. Buyer bid history includes `status`.
On /ws, auction.unsubscribe accepts listingId and returns an ack. Subscribe returns a versioned snapshot before subsequent transient events. Reconnect with a new bearer token when the JWT expires.
Alerts are durable before delivery. On replay, incomplete deliveries are retried with the same eventId; deduplicate live events by that ID.
On /ws, nearby.subscribe returns the current nearby feed as its ack. listing.available and listing.unavailable stream changes while subscribed. Use nearby.unsubscribe to stop. GET /api/listings remains the reconnect path.
Register a device with POST /api/me/devices {platform: ANDROID or IOS, token: provider token}. DELETE /api/me/devices/{id} removes an owned registration. Offline pushes default to logging. Enable TOLMOL_PUSH_FCM_ENABLED with FCM_PROJECT_ID, FCM_CLIENT_EMAIL, FCM_PRIVATE_KEY (service account with messaging permission). Enable TOLMOL_PUSH_APNS_ENABLED with APNS_TEAM_ID, APNS_KEY_ID, APNS_BUNDLE_ID, APNS_PRIVATE_KEY and APNS_SANDBOX (true for development). Use secret-injected PKCS#8 PEM keys; literal backslash-n line separators are supported.
GET /api/notifications accepts before (last row cursor) and limit (1–100). Each row includes cursor and read. GET /api/notifications/unread-count, PUT /api/notifications/{eventId}/read and PUT /api/notifications/read-all operate on your inbox. PUT /api/notifications/preferences/chat with {"muted":true} or false. PUT/DELETE /api/notifications/preferences/listings/{listingId} mute/unmute that lot.

## Deployment

1. Install JDK 21 and Docker. Copy `.env.example` to `.env` for a new setup. Supply independent random values for `TOLMOL_DB_PASSWORD` and `TOLMOL_JWT_SECRET` (at least 32 bytes for JWT). Keep an existing volume’s original database credentials. Leave push disabled for local smoke tests.
2. From `backend/` run `.\gradlew.bat bootJar`, then `docker compose --profile app up --build -d`. On Linux use `./gradlew bootJar`. The Dockerfile packages the executable JAR and runs Java as an unprivileged user. Test `GET /health` at localhost on `TOLMOL_HTTP_PORT` (default 8080).
3. On a fresh volume Compose initializes the four databases. On each application start Flyway applies pending migrations in each. External PostgreSQL requires provisioned databases and a migration-capable user. Set individual JDBC URLs using the variables in `.env.example` or use the shared URL prefix. Host-mode Redis and Kafka endpoints are configurable; the Compose app uses internal service names.
4. For TLS, point your domain at the host and configure a reverse proxy to forward HTTPS and WebSocket upgrades. Example Caddyfile for a proxy **on the host**:

```caddyfile
api.example.com {
    reverse_proxy 127.0.0.1:8080
}
```

**Smoke test.** Register one seller and one buyer, log in and update both locations within 20 km. Publish a lot, find it in discovery, bid, and have the seller accept. Open the winning thread, send a message via /ws, and check recipient delivery. Inspect the weight and confirm the deal as the other participant. Check that the deal is COMPLETED. Test live provider delivery separately with the app closed on a registered device.

## SCRAP PHOTOS

Original supplied photos are in scrap_photos/. Add more with a category prefix (metal_, paper_, cardboard_, plastic_, paint_, electronics_, furniture_, other_), then run from the repository root:

```powershell
pwsh -File tools/import_images/import.ps1
```

The rerunnable importer keeps a hash manifest and stable image keys, skips previously imported photos, scales the longest side to <=800px and writes JPEGs <=120KB to drawable-nodpi (native WebP encoding is unavailable). Originals are unchanged. Unknown categories map to OTHER. No eBay screenshots, web downloads or network image loader are included. These supplied photos have no recorded license: confirm your right to use them before a public release.


## DEMO DATA (local only)

Stop a running backend first. With Docker infrastructure running, start from backend/:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=demo"
```

Alternatively pass --tolmol.seed.demo=true. Normal startup does not seed. Never enable demo in production; prod/production profiles reject it. Every demo startup refreshes open auction deadlines and user locations. The seeder is idempotent and keeps existing bids and terminal deals.

Set the emulator location to latitude **19.0760**, longitude **72.8777**, send it, and allow location in TolMol. The optional properties --tolmol.seed.center-lat and --tolmol.seed.center-lng move the cluster (send that same center to the emulator). Demo users are 0.3–7.5 km around the center.

All 20 demo accounts use **Demo@12345**. On the welcome screen choose Create an account, then Already have an account? Sign in to use email/password. These are local email accounts, not Google accounts.

| Login | Main activity |
| --- | --- |
| demo.user01@tolmol.demo | Seller: my listings and received bids |
| demo.user09@tolmol.demo | Buyer: feed, bids, six chats and three deal states |
| demo.user17@tolmol.demo | Both: switch between buying and selling |

There are 50 open lots plus 3 awarded lots supporting PENDING_PICKUP, WEIGHT_DISPUTE and COMPLETED deals. Thirty open lots have 1–4 bids, with normal minimum increments; historical pair bids respect the six-hour cooldown. All accounts retain the standard buyer and seller roles.

Reset and reseed only demo data (stop the backend before rerunning):

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=demo --tolmol.seed.reset=true"
```

For removal without reseeding also pass --tolmol.seed.reset-only=true. Reset scopes all deletes and cache cleanup to accounts ending @tolmol.demo and their related data. It refuses deletion if demo bids touch another user's listing. Unrelated accounts and listings are preserved; no database or Redis flush is used. Restart normally afterwards to leave demo disabled.

Discovery uses the supplied scrap photos and TolMol's dark pixel theme. Search matches title, category and description locally; category chips, watchlist hearts, ending-soon sections, section grids and recent views work without a network image service. Recently viewed IDs last for the current app session. Listing details show approximate distance and public seller name/member date; pickup coordinates remain participant-only. Publish supports a title and optional description with category selection; image upload remains outside MVP scope.
