create table certificates (
  id uuid primary key,
  tenant_id varchar(100) not null,
  name varchar(255) not null,
  common_name varchar(255),
  issuer varchar(255),
  serial_number varchar(255),
  sha256_fingerprint varchar(255),
  not_before timestamp with time zone,
  not_after timestamp with time zone,
  status varchar(50) not null,
  renewal_status varchar(50) not null,
  owner varchar(255),
  notes text,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null
);

create index idx_certificates_tenant_id on certificates(tenant_id);
create index idx_certificates_status on certificates(status);
create index idx_certificates_not_after on certificates(not_after);