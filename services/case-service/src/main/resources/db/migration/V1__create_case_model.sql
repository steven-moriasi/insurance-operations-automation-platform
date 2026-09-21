create table policy_context (
    id uuid primary key,
    policy_number varchar(64) not null unique,
    product_code varchar(64) not null,
    status varchar(32) not null,
    cover_start_date date not null,
    cover_end_date date not null,
    currency varchar(3) not null
);

create table party (
    id uuid primary key,
    party_type varchar(32) not null,
    external_reference varchar(96) not null unique,
    full_name varchar(160) not null,
    phone_number varchar(32),
    email_address varchar(254)
);

create table claim_case (
    id uuid primary key,
    claim_reference varchar(64) not null unique,
    policy_id uuid not null references policy_context(id),
    claimant_party_id uuid not null references party(id),
    loss_date date not null,
    reported_at timestamp with time zone not null,
    status varchar(40) not null,
    owner_username varchar(128),
    updated_at timestamp with time zone not null,
    version bigint not null default 0
);

create index idx_claim_case_owner on claim_case(owner_username, updated_at);
create index idx_claim_case_status on claim_case(status, updated_at);

create table case_task (
    id uuid primary key,
    claim_id uuid not null references claim_case(id),
    task_type varchar(64) not null,
    status varchar(32) not null,
    assignee varchar(128),
    due_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    completed_at timestamp with time zone
);

create index idx_case_task_claim on case_task(claim_id, created_at);
create index idx_case_task_assignee on case_task(assignee, status, due_at);

create table evidence_metadata (
    id uuid primary key,
    claim_id uuid not null references claim_case(id),
    evidence_type varchar(64) not null,
    object_key varchar(512) not null unique,
    media_type varchar(128) not null,
    size_bytes bigint not null,
    sha256 varchar(64) not null,
    status varchar(32) not null,
    submitted_at timestamp with time zone not null
);

create index idx_evidence_claim on evidence_metadata(claim_id, submitted_at);

create table assessment (
    id uuid primary key,
    claim_id uuid not null references claim_case(id),
    assessor_username varchar(128) not null,
    recommended_amount numeric(19, 2) not null,
    currency varchar(3) not null,
    outcome varchar(48) not null,
    rationale varchar(2000) not null,
    submitted_at timestamp with time zone not null
);

create index idx_assessment_claim on assessment(claim_id, submitted_at);

create table claim_decision (
    id uuid primary key,
    claim_id uuid not null references claim_case(id),
    assessment_id uuid not null references assessment(id),
    requested_by varchar(128) not null,
    amount numeric(19, 2) not null,
    currency varchar(3) not null,
    status varchar(32) not null,
    decided_by varchar(128),
    reason varchar(2000) not null,
    requested_at timestamp with time zone not null,
    decided_at timestamp with time zone
);

create index idx_decision_claim on claim_decision(claim_id, requested_at);

create table settlement (
    id uuid primary key,
    claim_id uuid not null references claim_case(id),
    decision_id uuid not null references claim_decision(id),
    amount numeric(19, 2) not null,
    currency varchar(3) not null,
    status varchar(32) not null,
    external_reference varchar(128),
    instructed_at timestamp with time zone not null,
    reconciled_at timestamp with time zone
);

create index idx_settlement_claim on settlement(claim_id, instructed_at);

create table audit_event (
    id uuid primary key,
    aggregate_type varchar(64) not null,
    aggregate_id uuid not null,
    event_type varchar(96) not null,
    actor_username varchar(128) not null,
    occurred_at timestamp with time zone not null,
    event_data text not null
);

create index idx_audit_aggregate on audit_event(aggregate_id, occurred_at);
