alter table certificates
  add column if not exists created_by varchar(255),
  add column if not exists updated_by varchar(255);

alter table assets
  add column if not exists created_by varchar(255),
  add column if not exists updated_by varchar(255);
