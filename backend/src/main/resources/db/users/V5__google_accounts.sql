alter table accounts alter column password_hash drop not null;
alter table accounts add column google_subject varchar(255) unique;
alter table accounts add column email_verified boolean not null default false;
-- Preserve identities created through the earlier social endpoint.
update accounts a set google_subject = s.subject, email_verified = true
from social_identities s where s.user_id = a.id and s.provider = 'GOOGLE';
