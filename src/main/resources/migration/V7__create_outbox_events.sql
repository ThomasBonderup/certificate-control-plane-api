create table outbox_events (
    id uuid primary key,
    aggregate_type varchar(100) not null,
    aggregate_id uuid not null,
    event_type varchar(255) not null,
    topic varchar(255) not null,
    event_key varchar(255) not null,
    payload jsonb not null,
    created_at timestamptz not null,
    published_at timestamptz,
    status varchar(50) not null
);

create index idx_outbox_events_status_created_at
    on outbox_events(status, created_at);

create index idx_outbox_events_aggregate_id
    on outbox_events(aggregate_id);