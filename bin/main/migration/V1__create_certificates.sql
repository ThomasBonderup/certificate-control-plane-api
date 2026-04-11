create table certificates {
  id uuid primary key,
  tenant_id varchar(100) not null,
  name varchar(255) not null,
  common_name varchar(255),
  issuer varchar(255),
  serial_number varchar(255),
  sha256_fingerprint varchar(255),
  not_before timestamptz,
  not_after timestamptz,
  status varchar(50) not null,
  renewal_status(50) not null,
  owner varchar(255),
  notes text,
  created_at timestamptz not null,
  updated_at timestamptz not null
}