create table integration_attempt (
    id uuid primary key,
    idempotency_key varchar(128) not null unique,
    operation_type varchar(64) not null,
    subject_reference varchar(128) not null,
    request_hash varchar(64) not null,
    status varchar(32) not null,
    response_json text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0
);

create index idx_integration_attempt_subject
    on integration_attempt(operation_type, subject_reference, created_at);

create table integration_outbox (
    id uuid primary key,
    aggregate_type varchar(64) not null,
    aggregate_id varchar(128) not null,
    event_type varchar(128) not null,
    schema_version integer not null,
    payload_json text not null,
    occurred_at timestamp with time zone not null,
    published_at timestamp with time zone,
    attempts integer not null default 0,
    version bigint not null default 0
);

create index idx_integration_outbox_pending
    on integration_outbox(published_at, occurred_at);
