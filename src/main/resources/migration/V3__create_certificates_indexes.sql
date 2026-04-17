create index idx_certificates_tenant_id on certificates(tenant_id);
create index idx_certificates_status on certificates(status);
create index idx_certificates_not_after on certificates(not_after);