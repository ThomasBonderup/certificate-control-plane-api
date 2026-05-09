create table certificate_renewal_status_history (
  id uuid primary key,
  certificate_id uuid not null references certificates(id) on delete cascade,
  tenant_id varchar(255) not null,
  old_renewal_status varchar(100),
  new_renewal_status varchar(100) not null,
  blocked_reason text,
  updated_by varchar(255),
  occurred_at timestamptz not null,
  created_at timestamptz not null
);

create index idx_certificate_renewal_status_history_certificate_id
  on certificate_renewal_status_history(certificate_id);

create index idx_certificate_renewal_status_history_tenant_id
  on certificate_renewal_status_history(tenant_id);

create index idx_certificate_renewal_status_history_occurred_at
  on certificate_renewal_status_history(occurred_at);
