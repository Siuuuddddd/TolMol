create function emit_listing_availability() returns trigger language plpgsql as $$
declare
 event_id uuid := gen_random_uuid();
 kind text := case when new.status='OPEN' then 'listing.available' else 'listing.unavailable' end;
begin
 if tg_op='UPDATE' then
   if old.status<>'OPEN' or new.status='OPEN' then return new; end if;
 end if;
 insert into outbox(id,aggregate_id,event_type,payload) values(event_id,new.id,kind,
   jsonb_build_object('id',event_id,'type',kind,'aggregate',new.id,'audience','[]'::jsonb,
    'data',jsonb_build_object('status',new.status,'category',new.category,'weight',new.weight::text,
    'startingRate',new.starting_rate::text,'version',new.version::text,'deadline',new.deadline::text))::text);
 return new;
end $$;
create trigger listing_availability after insert or update of status on listings
 for each row execute function emit_listing_availability();
