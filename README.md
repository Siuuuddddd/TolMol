# TolMol backend

TolMol is a Kotlin and Spring Boot modular monolith for local scrap auctions. It uses PostgreSQL for identity, locations, marketplace and chat, Redis for caches, Kafka for events, and one authenticated WebSocket for live updates.

## Run locally

Install JDK 21 and start Docker Desktop with Linux containers. From the repository root in PowerShell, run the following once to create local development credentials. The application and Compose both read the ignored `.env` file.

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

## REST API

Send JSON bodies and use `Authorization: Bearer <accessToken>` on authenticated requests. Currency fields are decimal rupees, rates are rupees per kilogram, weights are kilograms, deadlines are ISO-8601 instants, and IDs are UUIDs except the numeric chat message cursor.

| Endpoint | Purpose |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | Register with email, password and roles such as `["SELLER","BUYER"]`. Login with email and password for a token. |
| `PUT /api/me/location/{BUYER or SELLER}` | Set latitude and longitude for that role. Refresh within 15 minutes before publishing, browsing or bidding. |
| `POST /api/listings` | Publish with category, weight, startingRate and deadline. Uses the seller’s stored location. |
| `GET /api/listings`, `GET /api/listings/watchlist` | Nearby open lots and nearby watched lots. |
| `PUT` or `DELETE /api/listings/{id}/watch` | Watch or unwatch a lot. |
| `POST /api/listings/{id}/bids` | Submit `rate` and a UUID `requestKey`. Reuse the key when retrying. |
| `GET /api/listings/{id}/auction`, `GET /api/listings/{id}/bids` | Read the auction snapshot or, as seller, the list of offers. |
| `POST /api/listings/{id}/accept` | Seller awards a recorded `bidId`. At the deadline the backend awards the highest bid automatically, or expires an empty auction. |
| `POST /api/listings/{id}/chat` | Buyer opens or retrieves their thread with the seller. Returns the thread ID. |
| `GET /api/chat/{threadId}/messages` | Latest 50 messages. Supply `?after=0` to start from the oldest, then pass the last message ID for the next page. |
| `GET /api/notifications` | Current user’s most recent 100 alerts. |
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

Commands return `ack` with data or `error` with a status. Incoming events contain `eventId`, `type`, `aggregate` and `data`. Deduplicate by event ID. After reconnecting, resubscribe and fetch the latest auction snapshot, chat history or alert inbox. Offline push uses a logging stub. Payment confirmation records an agreement between participants and does not transfer funds.
