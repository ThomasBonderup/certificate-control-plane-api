create table assets (
  id uuid primary key,
  tenant_id varchar(100) not null,
  name varchar(255) not null,
  asset_type varchar(100) not null,
  environment varchar(100),
  hostname varchar(255),
  location varchar(255),
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null
);

create index idx_assets_tenant_id on assets(tenant_id);
create index idx_assets_asset_type on assets(asset_type);
create index idx_assets_environment on assets(environment);
create index idx_assets_hostname on assets(hostname);

create table certificate_bindings (
    id uuid primary key,
    certificate_id uuid not null references certificates(id) on delete cascade,
    asset_id uuid not null references assets(id) on delete cascade,
    binding_type varchar(100) not null,
    endpoint varchar(255),
    port integer,
    created_at timestamp with time zone not null,

    constraint uq_certificate_asset_binding unique (
        certificate_id,
        asset_id,
        binding_type,
        endpoint,
        port
    )
);

create index idx_certificate_bindings_certificate_id
    on certificate_bindings(certificate_id);

create index idx_certificate_bindings_asset_id
    on certificate_bindings(asset_id);

create index idx_certificate_bindings_binding_type
    on certificate_bindings(binding_type);
