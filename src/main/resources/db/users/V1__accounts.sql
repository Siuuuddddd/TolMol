create table accounts (
 id uuid primary key, email text not null unique, password_hash text not null,
 roles text not null
);
create schema buyer;
create schema seller;
