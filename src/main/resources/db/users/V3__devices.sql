create table devices (
 id uuid primary key, owner_id uuid not null references accounts, platform text not null, token text not null,
 unique(platform,token)
);
create index devices_owner on devices(owner_id);
