create table threads (
 id uuid primary key, listing uuid not null, seller uuid not null, buyer uuid not null,
 unique(listing,buyer), check(seller<>buyer)
);
create table messages (
 id bigserial primary key, thread_id uuid not null references threads, sender uuid not null,
 body text not null, client_id uuid not null, sent_at timestamptz not null default now(),
 unique(thread_id,sender,client_id)
);
create index messages_history on messages(thread_id,id);
