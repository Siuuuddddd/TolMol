create table deals (
 listing uuid primary key references listings, seller uuid not null, buyer uuid not null, rate numeric not null,
 estimated_weight numeric not null, actual_weight numeric, amount numeric, proposed_by uuid, revision bigint not null default 0,
 state text not null default 'PENDING_PICKUP' check(state in ('PENDING_PICKUP','WEIGHT_DISPUTE','COMPLETED','CANCELLED_RELISTED')),
 relisted uuid references listings
);
create index deals_seller on deals(seller);
create index deals_buyer on deals(buyer);

create function publish_deal_transition() returns trigger language plpgsql as $$
declare
 event_id uuid := gen_random_uuid();
 kind text;
 fields jsonb;
begin
 if tg_op='INSERT' or new.state is distinct from old.state then
    kind := 'DEAL_' || new.state;
 else
    kind := 'DEAL_UPDATED';
 end if;
 fields := jsonb_strip_nulls(jsonb_build_object('state',new.state,'revision',new.revision::text,
    'actualWeight',new.actual_weight::text,'amount',new.amount::text,'relisted',new.relisted::text));
 insert into outbox(id,aggregate_id,event_type,payload) values(event_id,new.listing,kind,
    jsonb_build_object('id',event_id,'type',kind,'aggregate',new.listing,
      'audience',jsonb_build_array(new.buyer,new.seller),'data',fields)::text);
 return new;
end $$;
create trigger deal_events after insert or update on deals for each row execute function publish_deal_transition();

create function open_awarded_deal() returns trigger language plpgsql as $$
begin
 insert into deals(listing,seller,buyer,rate,estimated_weight)
 select new.id,new.seller,b.buyer,b.rate,new.weight from bids b where b.id=new.winning_bid;
 return new;
end $$;
create trigger open_deal after update of winning_bid on listings for each row
 when (old.winning_bid is null and new.winning_bid is not null) execute function open_awarded_deal();

insert into deals(listing,seller,buyer,rate,estimated_weight)
 select l.id,l.seller,b.buyer,b.rate,l.weight from listings l join bids b on b.id=l.winning_bid;
