create table outbox (
 seq bigserial primary key, id uuid not null unique, aggregate_id uuid not null,
 event_type text not null, payload text not null, published boolean not null default false
);
create index outbox_pending on outbox(seq) where not published;
