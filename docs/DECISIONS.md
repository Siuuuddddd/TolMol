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
