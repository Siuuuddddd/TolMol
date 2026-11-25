create table account_profiles (
 user_id uuid primary key references accounts(id),
 name varchar(100) not null
);
create table social_identities (
 provider varchar(16) not null,
 subject varchar(255) not null,
 user_id uuid not null references accounts(id),
 primary key(provider,subject)
);
