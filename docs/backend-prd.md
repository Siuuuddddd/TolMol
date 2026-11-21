# TolMol backend product requirements

**Status** — Working draft. Confirmed product rules below guide the build. The final section records decisions still needed.

## Purpose and scope

TolMol connects people selling scrap with buyers who can collect it locally. A seller publishes a lot, nearby buyers ask questions and bid, and one buyer wins. The backend supports this journey through discovery, identity, listings, bidding, chat, watchlists and notifications. The mobile app consumes the APIs and WebSocket connections. Frontend implementation is deferred.

## Marketplace rules

| Stage | Required behavior |
|---|---|
| Publish | A seller identifies the scrap category, quantity in kilograms, starting price per kilogram, location and auction deadline. Each bid quotes a price per kilogram for that lot’s category. For example, a ₹12/kg bid on an estimated 20 kg of paper implies an estimated ₹240 total. |
| Discover | Use the buyer’s current location. Only buyers within 20 km of the seller can discover the lot or bid on it. Check eligibility again when receiving a bid, even when the buyer previously found or watchlisted the lot. |
| Ask | A nearby buyer can initiate a private conversation with the seller before placing a bid. They can exchange messages until the item is sold. Each buyer’s conversation is private to that buyer and the seller. |
| Bid | Apply a roughly two-hour cooldown to the individual buyer. Other eligible buyers can continue bidding during that interval. Return the accepted price per kilogram or a specific rejection reason. For a cooldown rejection, return when the buyer can bid again. |
| Award early | Before the deadline, the seller can accept any valid bid on their lot, including a lower bid whose terms they prefer. Acceptance finalizes the winning bid and closes bidding immediately. |
| Close at deadline | If the seller has not chosen a bid, award the lot to the highest valid bidder. An auction with zero bids expires without a winner, following the handoff. |
| Notify | Deliver auction updates to the seller, relevant buyers and users who watchlist the lot. Support live delivery over WebSockets and push when the app is closed. Deliver private message alerts only to the other conversation participant. |

## Backend guarantees

- One identity can buy and sell. Authorize each action against its user and lot. Only the owner can accept a bid, and the bid must belong to that lot.
- Use PostgreSQL to decide bid eligibility, the highest bid and the winner. Concurrent bidding, a seller’s acceptance and deadline processing must produce at most one award. Once the deadline passes, the automatic award rule applies even if the expiry worker runs late.
- Retrying the same bid request returns its original outcome without recording a second bid or extending the cooldown. Commit auction changes and their publication records together. Persist chat messages and their publication records together.
- Publish committed changes through Kafka. Include an auction version with the current price, leader information appropriate to the recipient, and status. Support fetching the latest snapshot after a missed update and retrieving missed chat messages after reconnecting.
- Route overlapping notification subscriptions or replayed events without creating duplicate user alerts. Keep exact locations and private conversations restricted to authorized users.

## Fixed architecture

Kotlin and Spring Boot power one deployable modular monolith with Discovery, User, Buying, Selling, Bidding, Chat and Notification modules. Use PostgreSQL databases for users, marketplace, location and chat. The Users database keeps buyer and seller activity separate under the shared identity. Redis caches nearby listing and location data, current auction state and recent chat history. Kafka carries committed events. Clients submit bid commands through the API and receive live bids, chat and alerts through the shared WebSocket gateway. Background notifications use mobile push delivery.

## Acceptance examples

1. A buyer 19 km away can find an open lot. If their current location is 21 km away when they bid, the backend rejects the bid and leaves the auction unchanged.
2. Two buyers bid ₹18/kg and ₹20/kg. Before the deadline, the seller can award the lot to the ₹18/kg buyer. If the seller takes no action, the ₹20/kg buyer wins at the deadline.
3. A bid response is lost. A retry with the same request key yields one recorded bid, one cooldown and one logical bid event.
4. A buyer with no bids can start a conversation. A watchlisted buyer outside that conversation receives no private message or message preview.
5. An acceptance request and deadline processing overlap. The auction records one winner and every recipient receives a result consistent with that award.

## Decisions to settle

- **Cooldown** — Is the interval exactly two hours across all lots, or two hours per buyer per lot? Does only a successful bid start it?
- **Bid validity** — The handoff used a minimum increment. Can a new bid come in below the current leader to offer better collection terms? Decide how buyers attach terms to a bid and how equal highest bids are resolved at expiry.
- **Location** — Define how recent a location must be, which seller location anchors a published lot, and what a watcher who moves outside the radius may learn from alerts.
- **Award and sale** — Does an award immediately mark the item sold and close chat, or can the winner and seller keep chatting until pickup? Define when exact pickup details become available and whether the backend must record final weight, payment or completion in the first release.
- **Notification audience** — Specify whether all previous bidders or only affected bidders receive each bid alert. Confirm which events trigger background push for watchers, including reminders.
