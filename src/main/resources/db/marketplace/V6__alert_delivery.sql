alter table alerts add column delivered boolean not null default true;
alter table alerts alter column delivered set default false;
