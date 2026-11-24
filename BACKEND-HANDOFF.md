# TolMol backend handoff

The backend lives in `backend/`. The Android project lives in `android/` and is described in [ANDROID-HANDOFF.md](ANDROID-HANDOFF.md).

**Baseline**. The original MVP and all seven backend follow-up items shipped by [`ecc1de9`](https://github.com/Siuuuddddd/TolMol/tree/ecc1de9). The current backend also exposes the authenticated account and role profile, bid-history status, and participant-only conversation writability used by the Android client.

The [PRD](docs/PRD.md) owns product behavior. [DECISIONS.md](docs/DECISIONS.md) explains implementation choices. The [README](README.md) contains local setup, endpoint paths, request fields and WebSocket examples.

## What works today

| Area | Implemented behavior |
|---|---|
| Identity and location | Email/password registration, bcrypt, one-hour bearer JWTs, and one account with buyer and seller roles. Location is stored per user and role. Publishing, discovery and bidding require a location refreshed within 15 minutes. |
| Listings and discovery | A seller publishes category, estimated kilograms, opening rate in rupees/kg and a deadline. The lot retains its pickup coordinates. Buyers discover open lots within 20 km and can watch or unwatch them. The nearby feed omits exact pickup coordinates. |
| Bidding | The first bid must meet the opening rate. Each later bid must exceed the current highest. A successful bid starts a six-hour cooldown for that buyer across **all lots from the same seller**. Other sellers are immediately eligible. Retrying the same bid with the same request key returns the original result. |
| Award | Before the deadline the seller can accept **any recorded valid bid**, including an earlier lower offer with preferable pickup terms. At expiry the highest bidder wins automatically. An empty auction expires. The award and pending deal are recorded in the same Marketplace DB transaction. |
| Chat | A nearby buyer can initiate a private thread before bidding. Both participants can read history and send messages. After award only the winning thread stays writable through pickup. History supports fetching after a numeric message ID. |
| Notifications | Sellers, bidders and watchers receive auction changes. Individual bid outcomes, chat and deal changes are scoped to their recipients. Connected users receive WebSocket events. The inbox supports cursor pagination, unread counts and read actions. Preferences suppress chat or listing delivery. Offline push supports configurable FCM/APNs adapters and logs by default. |
| Pickup and settlement | Only the seller and winning buyer can read deal pickup coordinates. The buyer records the measured weight. A match calculates accepted rate × weight, and the seller confirms settlement. A mismatch opens a dispute. Either participant can propose a revised **total** and the other must confirm that revision. Either can cancel an unresolved dispute. Cancellation atomically creates a new listing with corrected weight and a fresh 24-hour deadline. |

Deal states are `PENDING_PICKUP`, `WEIGHT_DISPUTE`, `COMPLETED` and `CANCELLED_RELISTED`. Payment is a manual acknowledgment. Preserve the original bid, auction and deal when relisting.
| Personal history | Authenticated inventory, bid history and conversation pages accept an exclusive UUID cursor and expose only the caller’s records. |
| Live subscriptions | Subscribe and unsubscribe per socket for auctions or nearby discovery. Acknowledgments include a snapshot. The gateway synchronizes subscription setup with event delivery and closes sockets at JWT expiry. |
| Live nearby discovery | Publication, relisting and auction closure write availability events through the marketplace outbox. Transient delivery checks the buyer’s fresh location and the 20 km boundary. GET /api/listings provides catch-up. |
| Alert recovery and devices | Pending inbox deliveries resume on replay. Event IDs support deduplication. Users manage their own device tokens. Provider credentials are injected at runtime. |
| Deployment | Dockerfile and optional Compose app profile package the host-built JAR. See .env.example and README for migrations, TLS and the smoke checklist. |

## Where the behavior lives

One Kotlin/Spring Boot application runs on JDK 21. Source modules under `backend/src/main/kotlin/com/tolmol` are `user`, `marketplace`, `bidding`, `chat`, `notification`, `deal` and shared `platform`. The marketplace module currently implements the HLD’s discovery, buying/watchlist and selling/publication responsibilities. Follow the existing four layers: API controllers/DTOs/mappers/error handlers, services, pure domain models/errors, and infrastructure adapters/repositories. Keep SQL and Redis operations out of controllers.

| Owner | Durable data and delivery |
|---|---|
| Users DB | Shared accounts, roles and owned device registrations. The `buyer` and `seller` schemas hold role activity projected from Marketplace DB through a retryable relay. |
| Location DB | Current role-specific positions, timestamps and revisions. Redis caches coordinates by user, role and revision. A database freshness check guards cached reads. |
| Marketplace DB | Listings, watchers, bids, awards, deals, alerts, read/delivery state, mute preferences and the marketplace outbox. Redis GEO selects nearby candidates. Exact distance and auction state determine visibility. Versioned Redis snapshots hold current auction state. |
| Chat DB | Threads, participants represented by buyer/seller IDs, messages and the chat outbox. Redis caches the latest 50 messages per thread version. |

Bid commands use REST. The authenticated `/ws` connection accepts `auction.subscribe`, `auction.unsubscribe`, `nearby.subscribe`, `nearby.unsubscribe` and `chat.send`. Committed auction and deal events go to Kafka topic `auction.events`; committed messages go to `chat.message`. The notification consumer routes events and creates inbox entries. Chat text is excluded from the durable alert inbox. Flyway migrations are under `backend/src/main/resources/db/{users,location,marketplace,chat}`.

## Remaining manual checks and scope

- Supply platform credentials, register an actual device token and confirm a background notification on Android and iOS. Integration tests use the logging provider.
- The local Compose deployment was built and returned UP from /health. For a public deployment, supply a host, domain, TLS termination, secrets and backups, then run the documented journey against that environment.
- Payment processing, multi-instance socket routing and advanced operational tooling remain outside scope.

## Validation and working agreement

- The last full build at the baseline passed **18 tests with zero failures, errors or skips**. Coverage includes a REST journey through pickup, location and watchlist behavior, bid ordering and cooldowns, deadline awards, authenticated WebSocket chat, Kafka notification routing, negotiated settlement and corrected-weight relisting. The packaged app also returned `UP` from `GET /health` against local Compose. These are historical results for the stated commit.
- From `backend/`, start Docker Desktop with Linux containers, use the README to create the ignored local `.env`, run `docker compose up -d`, then `.\gradlew.bat bootRun`. The local API listens on port 8080. Tests start separate disposable PostgreSQL, Redis and Kafka containers.
- Run the relevant test class while iterating, then `.\gradlew.bat build` at the milestone boundary. Add focused regression tests for simultaneous competing bids and concurrent award/expiry when changing those transactions. LifecycleTest also covers competing offers, concurrent expiry and seller acceptance, and subscription races.
- Preserve the fixed stack, four database owners and package boundaries. Record new backend decisions in `docs/DECISIONS.md`, update endpoint documentation with behavior changes, and commit and push completed milestones to `main`. Real payment processing, advanced production hardening and mobile implementation were outside the agreed MVP.
