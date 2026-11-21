create table alerts (
 sequence bigserial primary key, event_id uuid not null, recipient uuid not null, kind text not null,
 aggregate_id uuid not null, data text not null, unique(recipient,event_id)
);
create index alerts_inbox on alerts(recipient,sequence desc);
