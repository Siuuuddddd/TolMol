create table role_activity_outbox (
    id bigserial primary key, role text not null, user_id uuid not null, listing_id uuid not null,
    action text not null, occurred_at timestamptz not null default now()
);
create function record_listing_activity() returns trigger language plpgsql as $$
begin
    insert into role_activity_outbox(role,user_id,listing_id,action) values ('seller',new.seller,new.id,'LISTED');
    return new;
end $$;
create trigger record_listing after insert on listings for each row execute function record_listing_activity();

create function record_watch_activity() returns trigger language plpgsql as $$
begin
    if tg_op='DELETE' then
        insert into role_activity_outbox(role,user_id,listing_id,action) values ('buyer',old.buyer,old.listing,'UNWATCHED');
        return old;
    end if;
    insert into role_activity_outbox(role,user_id,listing_id,action) values ('buyer',new.buyer,new.listing,'WATCHED');
    return new;
end $$;
create trigger record_watch after insert or delete on watchers for each row execute function record_watch_activity();
