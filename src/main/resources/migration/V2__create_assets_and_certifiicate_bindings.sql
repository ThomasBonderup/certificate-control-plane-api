create table certificate_bindings (
    id uuid primary key,
    certificate_id uuid not null references certificates(id) on delete cascade,
    asset_id bigint not null references public.assets(id),
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
