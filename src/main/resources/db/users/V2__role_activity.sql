create table buyer.activity (
    event_id bigint primary key, user_id uuid not null references accounts, listing_id uuid not null,
    action text not null, occurred_at timestamptz not null
);
create index buyer_activity_history on buyer.activity(user_id, occurred_at);
create table seller.activity (like buyer.activity including all);
alter table seller.activity add foreign key(user_id) references accounts;
