create table positions (
 user_id uuid not null, role text not null, latitude double precision not null,
 longitude double precision not null, updated_at timestamptz not null default now(),
 primary key(user_id,role)
);
