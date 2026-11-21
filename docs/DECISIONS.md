# Backend decisions

- Deploy one Spring Boot application. Use package boundaries for the modules, one PostgreSQL database per ownership area, and atomic local writes with an outbox for cross-module events.
- A recorded bid starts a six-hour buyer–seller cooldown. Reject equal and lower offers once a leading bid exists. The seller may accept an earlier valid offer.
- A weight discrepancy opens a dispute. Both participants must agree to the same revised total to complete the sale. Either can end unsuccessful negotiations and relist with corrected weight. A relist has a new identity and auction history.
