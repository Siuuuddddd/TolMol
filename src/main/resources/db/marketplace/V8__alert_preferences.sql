alter table alerts add column is_read boolean not null default false;
create table notification_preferences(user_id uuid primary key,mute_chat boolean not null);
create table muted_listings(user_id uuid not null,listing uuid not null,primary key(user_id,listing));
