
create table listing_photo (
 id uuid primary key, listing uuid not null references listings on delete cascade,
 position integer not null check(position between 0 and 4),
 content_type text not null, size bigint not null, storage_key text not null,
 is_cover boolean not null, created_at timestamptz not null default now(),
 request_key text not null, unique(listing,position), unique(listing,request_key)
);
create unique index listing_photo_cover on listing_photo(listing) where is_cover;
create table photo_file_cleanup(storage_key text primary key);
create function queue_photo_cleanup() returns trigger language plpgsql as $$
begin insert into photo_file_cleanup values(old.storage_key) on conflict do nothing; return old; end $$;
create trigger photo_cleanup after delete on listing_photo for each row execute function queue_photo_cleanup();
