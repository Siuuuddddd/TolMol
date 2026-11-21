# TolMol backend handoff

**Baseline:** [`main` at `ccad4b5`](https://github.com/Siuuuddddd/TolMol/tree/ccad4b5). All seven backend MVP milestones shipped. Continue from the current checkout after comparing it with this baseline. This handoff covers backend development only. Mobile development is deferred.

The [PRD](https://github.com/Siuuuddddd/TolMol/blob/ccad4b5/docs/PRD.md) owns product behavior. [DECISIONS.md](https://github.com/Siuuuddddd/TolMol/blob/ccad4b5/docs/DECISIONS.md) explains implementation choices. The [README](https://github.com/Siuuuddddd/TolMol/blob/ccad4b5/README.md) contains local setup, endpoint paths, request fields and WebSocket examples. The backlog below is an engineering assessment. Proposed features still need prioritization.

## What works today

| Area | Implemented behavior |
|---|---|
| Identity and location | Email/password registration, bcrypt, one-hour bearer JWTs, and one account with buyer and seller roles. Location is stored per user and role. Publishing, discovery and bidding require a location refreshed within 15 minutes. |
| Listings and discovery | A seller publishes category, estimated kilograms, opening rate in rupees/kg and a deadline. The lot retains its pickup coordinates. Buyers discover open lots within 20 km and can watch or unwatch them. The nearby feed omits exact pickup coordinates. |
| Bidding | The first bid must meet the opening rate. Each later bid must exceed the current highest. A successful bid starts a six-hour cooldown for that buyer across **all lots from the same seller**. Other sellers are immediately eligible. Retrying the same bid with the same request key returns the original result. |
| Award | Before the deadline the seller can accept **any recorded valid bid**, including an earlier lower offer with preferable pickup terms. At expiry the highest bidder wins automatically. An empty auction expires. The award and pending deal are recorded in the same Marketplace DB transaction. |
| Chat | A nearby buyer can initiate a private thread before bidding. Both participants can read history and send messages. After award only the winning thread stays writable through pickup. History supports fetching after a numeric message ID. |
| Notifications | Sellers, bidders and watchers receive auction changes. Individual bid outcomes, chat and deal changes are scoped to their recipients. Connected users receive WebSocket events. The inbox returns the latest 100 alerts. Offline push calls a logging stub. |
| Pickup and settlement | Only the seller and winning buyer can read deal pickup coordinates. The buyer records the measured weight. A match calculates accepted rate × weight, and the seller confirms settlement. A mismatch opens a dispute. Either participant can propose a revised **total** and the other must confirm that revision. Either can cancel an unresolved dispute. Cancellation atomically creates a new listing with corrected weight and a fresh 24-hour deadline. |

Deal states are `PENDING_PICKUP`, `WEIGHT_DISPUTE`, `COMPLETED` and `CANCELLED_RELISTED`. Payment is a manual acknowledgment. Preserve the original bid, auction and deal when relisting.

## Where the behavior lives

One Kotlin/Spring Boot application runs on JDK 21. Source modules under `src/main/kotlin/com/tolmol` are `user`, `marketplace`, `bidding`, `chat`, `notification`, `deal` and shared `platform`. The marketplace module currently implements the HLD’s discovery, buying/watchlist and selling/publication responsibilities. Follow the existing four layers: API controllers/DTOs/mappers/error handlers, services, pure domain models/errors, and infrastructure adapters/repositories. Keep SQL and Redis operations out of controllers.

| Owner | Durable data and delivery |
|---|---|
| Users DB | Shared accounts and roles. The `buyer` and `seller` schemas hold role activity projected from Marketplace DB through a retryable relay. |
| Location DB | Current role-specific positions, timestamps and revisions. Redis caches coordinates by user, role and revision. A database freshness check guards cached reads. |
| Marketplace DB | Listings, watchers, bids, awards, deals, alerts and the marketplace outbox. Redis GEO selects nearby candidates. Exact distance and auction state determine visibility. Versioned Redis snapshots hold current auction state. |
| Chat DB | Threads, participants represented by buyer/seller IDs, messages and the chat outbox. Redis caches the latest 50 messages per thread version. |

Bid commands use REST. The authenticated `/ws` connection accepts `auction.subscribe` and `chat.send`. Committed auction and deal events go to Kafka topic `auction.events`; committed messages go to `chat.message`. The notification consumer routes events and creates inbox entries. Chat text is excluded from the durable alert inbox. Flyway migrations are under `src/main/resources/db/{users,location,marketplace,chat}`.

## Backend gaps and suggested order

| Order | Gap observed in the baseline | Suggested next change and acceptance check |
|---|---|---|
| 1 | **Recovering a user’s own work.** There are APIs to list nearby lots, watches and deals, but no seller inventory or buyer bid-history endpoint. Sellers can list bids only if they already know the listing ID. Chat history also requires a known thread ID. | Add authenticated, paginated reads for my listings, my bids and my conversations. Start with seller inventory. A returning seller with no locally saved IDs should be able to find an active lot, inspect its offers and open its conversations. Test that a second seller sees none of the first seller’s records. |
| 2 | **WebSocket session lifecycle.** The gateway authenticates on connection. It stores subscriptions until disconnect and exposes no unsubscribe command. Subscription authorization and registration are separate steps, leaving a window for a newer auction version to be missed. | Add unsubscribe, enforce token expiry on long-lived sessions, and make subscription acknowledgments and snapshot/version recovery reliable. Test a bid concurrent with subscription and verify that unsubscribing stops transient updates while preserving intentional watchlist alerts. |
| 3 | **Alert delivery after a partial failure.** In `NotificationService.consume`, inbox insertion precedes socket/push delivery. If the process stops between those steps, replay finds the existing inbox row and skips delivery. The alert remains readable in the inbox. | Keep the unique recipient/event key and distinguish pending delivery from a completed attempt. Test a restart after inbox insertion, verify a subsequent delivery attempt, and confirm that the inbox contains one row. |
| 4 | **New nearby lots appear on the next REST fetch.** The existing WebSocket subscription is for one known auction. A buyer browsing an area cannot subscribe to newly published or relisted lots. | If live discovery is the next product priority, publish listing-availability changes through the marketplace outbox and add an authorized nearby subscription. Test nearby, distant and stale-location buyers. Keep pickup coordinates private and retain REST as the reconnect path. |
| 5 | **Real background delivery.** The push provider currently logs metadata. There is no device-token registration or provider delivery. | When closed-app notifications are required, add authenticated device registration/removal, token ownership and a provider adapter. Keep the logging implementation for local tests. Accept the feature only after a real test device receives an offline alert. |
| 6 | **Shared deployment and operations.** The verified setup is local Compose. Multi-instance WebSocket routing, performance validation and production operations were deferred for the MVP. | Before an external pilot, provision a shared test backend with TLS and managed secrets, verify migrations against a backup, and exercise the full flow there. Add operational complexity only as deployment needs justify it. |

## Validation and working agreement

- The last full build at the baseline passed **11 tests with zero failures, errors or skips**. Coverage includes a REST journey through pickup, location and watchlist behavior, bid ordering and cooldowns, deadline awards, authenticated WebSocket chat, Kafka notification routing, negotiated settlement and corrected-weight relisting. The packaged app also returned `UP` from `GET /health` against local Compose. These are historical results for the stated commit.
- From the repo root, start Docker Desktop with Linux containers, use the README to create the ignored local `.env`, run `docker compose up -d`, then `.\gradlew.bat bootRun`. The local API listens on port 8080. Tests start separate disposable PostgreSQL, Redis and Kafka containers.
- Run the relevant test class while iterating, then `.\gradlew.bat build` at the milestone boundary. Add focused regression tests for simultaneous competing bids and concurrent award/expiry when changing those transactions. The existing sequential business-rule tests are a starting point.
- Preserve the fixed stack, four database owners and package boundaries. Record new backend decisions in `docs/DECISIONS.md`, update endpoint documentation with behavior changes, and commit and push completed milestones to `main`. Real payment processing, advanced production hardening and mobile implementation were outside the agreed MVP.
