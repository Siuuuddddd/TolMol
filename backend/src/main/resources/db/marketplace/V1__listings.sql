create table listings (
 id uuid primary key, seller uuid not null, category text not null, weight numeric not null check(weight>0),
 starting_rate numeric not null check(starting_rate>0), latitude double precision not null, longitude double precision not null,
 deadline timestamptz not null, status text not null default 'OPEN', version bigint not null default 0
);
create index listings_open on listings(deadline) where status='OPEN';
create table watchers(listing uuid references listings, buyer uuid not null, primary key(listing,buyer));
