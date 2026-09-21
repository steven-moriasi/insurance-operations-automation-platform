create table document_object (
    id uuid primary key,
    idempotency_key varchar(128) not null unique,
    claim_reference varchar(64) not null,
    file_name varchar(255) not null,
    object_key varchar(512) not null unique,
    content_type varchar(128) not null,
    expected_sha256 varchar(64) not null,
    maximum_bytes bigint not null,
    scan_work_item_id uuid,
    observed_bytes bigint,
    status varchar(32) not null,
    rejection_reason varchar(256),
    created_at timestamp with time zone not null,
    scanned_at timestamp with time zone,
    version bigint not null default 0
);

create index idx_document_object_claim
    on document_object(claim_reference, created_at);

create table automation_work_item (
    id uuid primary key,
    idempotency_key varchar(128) not null unique,
    work_type varchar(64) not null,
    business_reference varchar(128) not null,
    payload_json text not null,
    status varchar(32) not null,
    available_at timestamp with time zone not null,
    lease_owner varchar(128),
    lease_token uuid,
    lease_expires_at timestamp with time zone,
    attempts integer not null,
    maximum_attempts integer not null,
    result_json text,
    last_error varchar(512),
    created_at timestamp with time zone not null,
    completed_at timestamp with time zone,
    version bigint not null default 0
);

create index idx_automation_work_ready
    on automation_work_item(status, available_at, created_at);
