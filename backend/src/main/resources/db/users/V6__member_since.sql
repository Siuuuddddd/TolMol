alter table accounts add column joined_at timestamptz;
alter table accounts alter column joined_at set default now();
