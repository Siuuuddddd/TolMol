create table bids (
 id uuid primary key, listing uuid not null references listings, buyer uuid not null, seller uuid not null,
 rate numeric not null check(rate>0), request_key uuid not null, created_at timestamptz not null,
 unique(buyer,request_key)
);
create index bids_by_lot on bids(listing,rate desc);
create index bids_by_pair on bids(buyer,seller,created_at desc);
alter table listings add column winning_bid uuid references bids;
create table outbox (
 seq bigserial primary key, id uuid not null unique, aggregate_id uuid not null,
 event_type text not null, payload text not null, published boolean not null default false
);
create index outbox_pending on outbox(seq) where not published;
create function record_bid_activity() returns trigger language plpgsql as $$
begin
 insert into role_activity_outbox(role,user_id,listing_id,action) values ('buyer',new.buyer,new.listing,'BID_PLACED');
 return new;
end $$;
create trigger record_bid after insert on bids for each row execute function record_bid_activity();
