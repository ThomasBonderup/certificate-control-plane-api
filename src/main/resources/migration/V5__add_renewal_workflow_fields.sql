alter table certificates
  add column if not exists blocked_reason varchar(255),
  add column if not exists renewal_updated_at timestamp with time zone;
