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

## Google sign-in contract update — 2026-11-25

- The latest request supersedes the earlier email-collision policy for Google. `/auth/google` links a verified Google email to an existing account once, preserves that account's password/profile/roles, and otherwise creates a passwordless account with both Android registration roles. Stable Google subject is the identity key on subsequent sign-ins; a different subject cannot replace a linked subject.
- V5 adds a unique nullable Google subject and verified-email flag and allows null password hashes, while retaining existing password authentication and the legacy social-identity records. Inserts name their columns to survive schema additions.
- GOOGLE_CLIENT_ID comes only from backend environment/local .env. The verifier contract lives in service so infrastructure depends inward; its Google JWT implementation lives in infra/security and accepts an injected decoder in focused tests. RSA-signed test tokens validate rejection of wrong audiences, signatures, expiry, issuer and unverified email without contacting Google.
- No existing IP rate limiter was present in the owning backend source. The new Google endpoint therefore receives its own Redis-backed 30-attempts-per-peer-IP-per-minute filter. It ignores client-supplied forwarding headers and fails closed if Redis is unavailable; other business endpoints and authentication rules are unchanged.

- Android Google configuration now comes exclusively from GOOGLE_WEB_CLIENT_ID in ignored local.properties, matching backend GOOGLE_CLIENT_ID. Credential acquisition is presentation-only Android code; ViewModels call a dedicated domain GoogleSignInUseCase and the data repository exchanges the ID token, fetches the profile, then saves the TolMol JWT. /auth/google is public to the interceptor so an old token cannot block sign-in. Facebook SDK/UI/callbacks are removed; existing Google/network/cancellation failures do not crash the UI.

## Shared typography and dark styling — 2026-11-25

- The old welcome logo/card labels were raster lettering; the existing native headings already used bundled Pixelify Sans. Use that closest existing pixel family everywhere under its bundled SIL Open Font License. core/ui/Typography.kt defines the family once for all 15 Material 3 styles; readable body sizes are 18/16/14sp and labels are at least 13sp. Feature code contains no fontFamily override or nested theme. Currency, weights and countdowns inherit the same typography.
- All screens retain their layouts and actions while inheriting the dark charcoal/metal/lime palette. Account forms now use dark factory artwork instead of the cobalt/cream override. Shared PixelButton/PixelOutlinedButton components control the cut-corner shape, minimum touch height and palette; list/detail forms, chats, deal actions and permission setup use them.
- Native text replaces all welcome lettering, including the logo, headings and informational card descriptions. The original graphics are preserved in android/app/src/main/res/drawable-nodpi/welcome_factory_blank.png (853x1844). Built-in ImageGen was used for this text-removal edit; the font remains bundled locally and no runtime image or font service is used.
- Final asset prompt: "Edit this exact app artwork into a TEXTLESS UI background/sprite atlas. Preserve the full composition, pixel art style, all positions, all original dimensions/aspect ratio, colors, lighting, metallic frames, lime ingot icons, arrows, bolts, conveyor, boxes, cans, lamp, pipes, lime button battlements and floor exactly. Remove ONLY every piece of text: the top TolMol logo and its black shadow, What brings you here?, Sell scrap and its subtitle, Buy scrap nearby and its subtitle, You can do both with one account., and Sign in. Fill removed letters seamlessly using the local background surface texture: dark factory wall at top, plain matching metal within cards, dark wall between cards and button, plain lime button face. Do NOT remove recycling symbols on boxes; they are graphics. Do not add new objects or labels. Do not move or resize any element. Target is the same precise 853x1844 portrait layout; preserve all region boundaries because an Android UI will place live pixel text over these exact positions. Output only the edited textless artwork."
- MVP completion audit: restructure, Koin/Nav3/network/session setup, auth/roles, location, nearby/watchlist, auction/bidding/live updates, publish/inventory/accepting offers, bid history, chat, notifications, deal flow and documentation all have implemented routes and state handling. This change does not alter their business behavior. There is no missing implementation milestone; live Google acceptance is the single credential-dependent check left.

## Local Google OAuth activation — 2026-11-26

- Use the supplied Web application OAuth client ID for both Android's server client ID and backend audience verification. The separately created Android OAuth client registers the package/signing certificate in Google's project; its client ID is not the server audience. Load only the public client_id field from the downloaded Web configuration and keep both local properties files ignored. No client secret is required, copied or committed.
- Live acceptance succeeded on Pixel 9 Pro: the Google icon opened the provider account chooser, identity consent returned an actual Google ID token, backend verification issued a TolMol session, DataStore saved it and role selection displayed both roles. Restarting the backend and rebuilding Android are required when changing these local client-ID values.

- Pixel-letter legibility correction (2026-11-26): the bundled Pixelify Sans file has a real wght axis from 400 to 700, but the original family registered only regular and allowed synthetic bold. Register the actual regular/medium/semibold/bold variations and disable synthesis so narrow openings such as lowercase c are preserved. Card/list titles use 20sp medium weight with 0.5sp tracking. The same single bundled family remains app-wide. assembleDebug passed and the updated welcome cards were visually checked on Pixel 9 Pro.

- Direct-home navigation (2026-11-26): remove the post-authentication role picker. Every successful password/Google login, registration and session restore selects BUYER when the backend grants it, otherwise the account's available role, so authenticated state is immediately renderable as home. Existing dual-role functionality moves to a direct mode toggle on the home header. Single-role accounts have no toggle, and unsupported roles remain rejected. No backend role or auction rules change.

## Discovery feed and demo data — 2026-11-26

- Added optional listing title/description/imageKey through the existing layers, plus a buyer-authorized detail endpoint. Public feed/detail include computed distance and seller display name, never pickup coordinates. Legacy category strings and requests remain accepted; new clients use the eight canonical categories. Member-since is nullable for old accounts whose creation date was never recorded. Corrected-weight relisting preserves content fields.
- The two eBay screenshots are layout references only and are never bundled. All 17 scrap photos are preserved as originals: 2 paper, 2 cardboard, 1 metal, 3 paint, 5 electronics and 4 other (textile offcuts/cement sacks). Drums/buckets are categorized as paint based on the supplied context. No furniture/plastic photo was supplied; original pixel vector category illustrations provide fallbacks. The native Windows JPEG encoder is used because WebP encoding is unavailable; each import is <=800px and <=120KB.


### Discovery feed and optional local demo data — 2026-11-26

- Keep the existing category String API for backward compatibility; publish offers the eight MVP categories and the feed groups unknown/legacy categories under OTHER. Search/filter/section composition stays in a domain use case. See all uses a local grid state in Discovery, preserving bottom navigation. Recent views store at most 20 IDs in session memory; no offline database was added.
- Use 50 OPEN demo lots plus 3 awarded lots (53 total), because the three required deal states cannot also be OPEN. Twenty email-verified accounts have standard BUYER and SELLER roles; 8 mainly sell, 8 mainly buy, 4 do both. Historical bids spread across the last 48 hours, increasing pair timestamps as needed to preserve six-hour cooldown and strict auction chronology. Existing live bid/accept/deal rules are unchanged.
- Demo seeding requires profile demo or tolmol.seed.demo=true, rejects prod/production, and refreshes open deadlines/locations at startup. Auction expiry waits for ApplicationReadyEvent so startup runners finish restoring deadlines before expiry evaluates them. Normal expiry rules and one-second interval are unchanged.
- Scoped reset deletes only demo-owned data and refuses mixed ownership bids on unrelated listings. It never flushes Redis or drops databases. Existing demo terminal actions are retained on a normal rerun.
- Photo count: 17 originals, 17 optimized JPEGs; PAPER 2, CARDBOARD 2, METAL 1, PAINT 3, ELECTRONICS 5, OTHER 4. Textile/cement are OTHER and used drums/buckets are PAINT. PLASTIC and FURNITURE use distinct local vector placeholders. The importer regenerates the stable key lookup; unknown keys also use a category placeholder.

- Demo center changes also update still-open pickup coordinates and Redis GEO members, so rerunning with a new configured city cannot leave lots stranded at the previous center. A regression assertion reseeds in a second city and checks all 50 open lots remain discoverable.

- Emulator testing exposed a Nav3 detail transition retaining a loading effect keyed only by route ID while its ViewModel owner changed. Detail/chat/offer/deal effects now include the ViewModel in their keys; conversation messages and composer were rechecked after rebuilding.

- Public listing DTO distance is rounded to 0.1 km as well as formatted in the app; precise pickup coordinates are exposed only by the existing participant-only deal endpoint.

### Additional metal product photos — 2026-11-26

Imported six supplied photos (two iron rods, two copper wire, two aluminium sheets), bringing the bundled catalog to 23 photos. Stable keys scrap_metal_02/03 map to iron rods, 04/05 to copper wire, and 06/07 to aluminium sheet offcuts. Each pair is used across the two corresponding demo lots. Demo startup refreshes their image keys on existing OPEN lots without resetting bids, chats or deals. Original files and watermarks are retained; optimized images keep the existing 800px/120KB limits.

### Utensil and brass fitting photos — 2026-11-26

Added four supplied originals and optimized bundled JPEGs, bringing the catalog to 27 photos. scrap_metal_08/09 are assigned to the kitchen utensil lots; scrap_metal_10/11 are assigned to the brass fitting lots. Each supplied photo is used by one corresponding repeated demo lot. Existing OPEN demo lots receive the image keys on startup without a data reset. The importer preserves originals and existing keys, and keeps optimized files within 800px and 120KB.

### Plastic, appliance and furniture photos — 2026-11-26

Added all 18 supplied photos: 4 plastic, 6 electronics and 8 furniture, bringing the bundled catalog to 45 photos. Bottle/crate pairs use plastic keys 01–04; refrigerator/fan/mixer pairs use electronics 06–11; chair/wardrobe/table/sofa pairs use furniture 01–08. All originals are preserved and all optimized images follow the 800px/120KB limits.

Some products have only one open demo lot, so detail now includes a swipeable local two-photo gallery with a page count. The selected feed thumbnail stays first. This uses bundled related-photo keys and needs no image service or backend schema change. Fans and sofa labels were made generic to fit both supplied photos; existing OPEN demo records refresh the seeded content and image keys on startup without resetting bids or deals.

### Google login after Docker restart — 2026-11-26

Investigated the generic Google verification error: port 8080 belonged to a Docker app container using the November 21 image and no GOOGLE_CLIENT_ID environment value. The saved backend and Android OAuth client IDs matched; the running container had never reloaded that configuration. Rebuilt bootJar and recreated only the app service with --build --force-recreate, then verified the runtime audience configuration matched Android and health returned UP. Retrying Google sign-in on the emulator successfully opened Discovery.

README Google troubleshooting now distinguishes host bootRun from container startup and includes the exact rebuild/recreation commands. Docker Desktop/container restart alone retains old image/environment settings; no OAuth secrets or IDs were added to tracked files.

### Product detail and native navigation redesign — 2026-11-26

Adapted the supplied item-page layout to TolMol: pinned Item toolbar, edge-to-edge local photo pager with count/watch overlays, title then seller/avatar/message row, prominent real per-kg price, estimated lot value, collection/weight rows, full-width bid/watch/message actions, a live auction card, item specifics, description/details dialogs and horizontal real nearby-lot cards. The current dark/lime palette and Pixelify Sans remain unchanged. Bid entry uses a native modal sheet and retains the existing ViewModel/use-case/idempotency flow; no backend rules changed.

Use five native tabs (Home, My TolMol, Search, Inbox, Selling), including on detail screens. My TolMol exposes bids, watches, messages and deals; Inbox opens existing alerts. Selling/Search change authorized roles and preserve the intended destination. The bar uses Google's original outlined VectorDrawables, 24dp icons, a 64dp content height and system-owned gesture insets. Source and Apache license are in android/UI-ICONS.md and android/GOOGLE-MATERIAL-ICONS-LICENSE.txt.

Reference commerce controls map to genuine TolMol actions. No basket, instant purchase, shipping promise, sponsored recommendations, synthetic popularity/watch counts or payment flow was invented. Nearby cards and prices are existing backend data; seller initials are generated from the actual display name, as in the supplied initial-avatar reference.

### Cream brick design system — 2026-11-27

The new request supersedes the dark/lime theme and single-font rule. The attachment folder contains only the written specification; the two new images were not supplied, so exact color sampling is unavailable. design_reference/README.md records this limitation. Use the supplied fallback palette: sky #0B3FA8–#2E86DE, navy #0B1630, red #E8212A, blue #1F55D6, yellow #FBBF1E, cream #FBF5E8, steel #8A93A0 and dark tile #1A1F2B.

The shared source of truth is core/ui/theme (Color, Shape/Dimens, Typography, TolMolTheme). Pixelify Sans 700 remains the closest bundled open-license pixel display face; Nunito Sans variable 400–900 is bundled for readable geometric body/labels/prices. Both are SIL OFL. Every Material text style and light color slot is explicit; dynamic color and system dark mode are not used.

Core controls live in core/ui/components: BrickButton/secondary/card/chip, OutlinedPixelText, TolMolTextField, PixelDialog/Snackbar, SkeletonBrick, EmptyState, yellow-underlined links, SocialSignInButton, hanging SignBoard, SteelTopBar/BrickBottomBar, hazard/stone footers and lightweight Canvas art. Tokens use 20dp page gutters, 32dp auth gutters, 12dp gaps, 56dp fields, 64dp actions, 2.5dp outlines, 5dp hard shadows, 12dp studs with 24dp gaps and 8dp control corners. All main components have Android Studio previews. Legacy PixelButton names delegate to the new controls for compatibility.
