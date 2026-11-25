# Backend decisions

- Deploy one Spring Boot application. Use package boundaries for the modules, one PostgreSQL database per ownership area, and atomic local writes with an outbox for cross-module events.
- A recorded bid starts a six-hour buyer–seller cooldown. Reject equal and lower offers once a leading bid exists. The seller may accept an earlier valid offer.
- A weight discrepancy opens a dispute. Both participants must agree to the same revised total to complete the sale. Either can end unsuccessful negotiations and relist with corrected weight. A relist has a new identity and auction history.
- Registration uses email, bcrypt and self-selected buyer/seller roles. Bearer JWTs expire in one hour. Locations older than 15 minutes must be refreshed. Set TOLMOL_DB_PASSWORD and a random TOLMOL_JWT_SECRET (at least 32 bytes) locally. PostgreSQL repositories use explicit SQL and Flyway migrations for the four databases.
- Discovery uses the pickup coordinates captured when the lot is listed. Redis GEO selects nearby candidates and the backend checks exact spherical distance, auction state and deadline. Feed results omit pickup coordinates.

## Milestone 3 HLD review

- Users DB holds shared identity and buyer/seller schemas. Location DB owns current role coordinates. Added the missing Redis location cache with a 15-minute TTL. Reads check the durable revision and freshness before using cached coordinates, so a move or cache failure cannot grant access using the previous location.
- Discovery, buying (watchlists) and selling (publication) share the marketplace module in this milestone. Marketplace DB owns listings and subscriptions. A listing retains its pickup point even if the seller moves. A buyer can remove a watch from anywhere; the watchlist returns open lots that are still nearby.
- Redis GEO stores one entry per indexed listing. Discovery repairs missing index entries from PostgreSQL and falls back to database coordinates when Redis is unavailable or outside its latitude range.
- Bidding owns committed auction events and versioned bid state; Chat DB owns message history and the chat outbox. Their Kafka topics, Redis caches and shared WebSocket delivery belong to milestones 4–6.
- Publication and watch changes append activity records transactionally in Marketplace DB. A retryable relay projects them into seller.activity and buyer.activity in Users DB. A unique event ID makes retries safe.
- Auctions publish auction.events keyed by listing ID. Versioned Redis snapshots cache the highest rate and a public leader alias. Recorded bids and leader changes are separate events at the same version. The seller may select any earlier valid offer. Automatic closure runs once per second.
- A nearby buyer opens a private thread per listing before or after bidding. After award only the winning conversation stays writable until completion or cancellation. Clients authenticate the WebSocket handshake with the same bearer JWT used for REST. Recent history is the latest 50 messages; a numeric message cursor retrieves older or missed pages.
- Kafka consumers deduplicate by recipient and event ID. Sellers, bidders and watchers get auction alerts; private bid outcomes go to affected buyers. Authorized viewers can subscribe without watchlisting. Chat text travels only to its recipient; inbox entries store the message reference. Offline delivery logs event metadata through the push interface.
- An accepted or automatically awarded bid creates a pending deal in the same Marketplace DB transaction. Only its participants can retrieve pickup coordinates. The winning buyer records the measured weight. An exact match uses accepted rate × weight, rounded to paise; seller confirmation acknowledges settlement. Any difference opens a weight dispute. Either participant can propose a revised total; the other confirms that exact revision after payment. Settlement is a manual acknowledgment.
- Either participant can cancel an unresolved weight dispute. Cancellation creates one new open lot with the inspected weight, original category, pickup point and starting rate, and a fresh 24-hour deadline. Old chats become read-only, bid history remains intact and the buyer–seller cooldown carries over. Deal events use the marketplace outbox and reach only its participants.
- Personal history uses principal-scoped UUID keyset pagination. Inventory uses the public listing DTO and excludes pickup coordinates.
- Subscription authorization, snapshot, registration and acknowledgment are serialized per socket against transient delivery. Watchlist alerts are independent. Sessions close at JWT expiration.
- Existing inbox rows are migrated as delivered. New rows start pending and are completed after socket or push dispatch. A crash after dispatch can repeat delivery without duplicating the inbox row.
- Database triggers cover publication, corrected-weight relisting, award and expiry in the same outbox transaction. Discovery events are transient, location-checked per delivery and exclude coordinates.
- Each platform token belongs to one owner in Users DB. Cross-account reassignment is rejected until removal. Enabled adapters send a generic notification and event identifiers. Real-device acceptance needs the corresponding credentials.
- Muted events remain available in the owner’s inbox. Chat and listing mutes suppress both socket and push dispatch; listing mutes also suppress transient discovery/auction updates.
- The optional Compose app profile packages a host-built bootJar with a Java 21 runtime. Flyway migrates the four provisioned databases during startup; TLS terminates at the external reverse proxy.

## Android client

- The app restores the DataStore JWT and loads roles from the authenticated GET /api/me endpoint. The local session ends at token expiry or when a protected request rejects it.
- Buyer bid history returns a status from the authoritative listing and winning bid, while preserving UUID pagination and account scoping.
- GET /api/chat/{id} exposes the participant-only conversation and its current writable state. It shares the authorization and writable predicate used by chat.send.
- Android uses SDK 37 with a minimum of 26. Debug builds allow local emulator traffic and request local-network access on Android 17. Release URLs require TLS.
- Location is sent for the active role on foreground entry and every five minutes while active. Screen reads remain subject to the server freshness check.
- Live updates use one bearer-authenticated WebSocket. The client serializes commands because the server replies lack correlation IDs. Reconnection reloads current state and renews the active auction subscription.
- The Pixel 9 Pro onboarding uses the Scrap Conveyor palette and original artwork. Native Compose text and controls support Android font scaling.
- Android starts Koin in the Application and binds feature repositories, use cases, and ViewModels in the Koin application module. Navigation 3 scopes the injected ViewModels to each entry. The JVM unit suite verifies the dependency graph without starting Android.
- The signed-out welcome uses the Scrap Conveyor artwork with native scalable pixel headings, smaller non-clickable text cards, explicit authentication actions and Google/Facebook buttons. Registration has no role chips; new Android accounts receive both existing roles. Role selection remains a separate authenticated action.
- Android Studio uses its bundled JDK for Gradle sync. Studio-generated daemon JVM criteria are local because pinning a different JDK forced toolchain provisioning during Windows IDE sync.
- Windows workspace setup repairs the pre-restructure root Gradle link to backend/ and saves separate local Java homes for backend and Android IDE imports. IDE JDK selections use GRADLE_LOCAL_JAVA_HOME to resolve the project configuration.

## Authentication redesign — 2026-11-25

- The registration form follows the supplied name/email/password layout, with native editable controls, password visibility, local validation, social options and a sign-in link. Original pixel mailbox artwork uses the supplied cobalt, red and cream palette. No published Terms document was supplied, so the app does not invent legal terms or require a meaningless agreement checkbox.
- `POST /api/auth/register` accepts optional name without changing legacy clients. Users DB migration V4 stores profiles and provider identities separately, preserving the accounts schema and existing marketplace rules.
- `POST /api/auth/social` is the only new endpoint. API DTO/controller, service/verifier contract and infrastructure adapter/repository retain the existing layers. The backend, not the device, verifies provider tokens before issuing a TolMol JWT.
- Google uses Credential Manager's authorized-account auto-selection with consent/account-choice fallback. Facebook uses its SDK with automatic analytics and advertising-ID collection disabled. No credential, secret, Firebase project or cloud deployment is bundled.
- Provider identity is keyed by provider/subject. A transaction and advisory lock make repeated sign-ins reuse one account; email collisions with existing accounts are rejected rather than silently linking identities. New social accounts have both existing roles and an unguessable password hash.
- Real Google/Facebook acceptance is blocked on owner-supplied provider registration and credentials. Email/password registration and the rest of the MVP remain usable; missing provider configuration returns an actionable error.

- Welcome restoration (2026-11-25): reuse regions of the original approved TolMol artwork for the full-width factory header, narrower non-clickable metal cards, lime button frame and hazard-striped floor. Remove the one-account note. Authentication actions retain their existing Google/Facebook and registration behavior. Original card lettering is preserved at normal text size, with native readable cards and button text at enlarged Android font settings. No backend or authentication contract changes.
