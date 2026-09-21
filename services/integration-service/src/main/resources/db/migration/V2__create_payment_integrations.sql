create table payment_instruction (
    id uuid primary key,
    idempotency_key varchar(128) not null unique,
    claim_reference varchar(64) not null,
    amount numeric(19, 2) not null,
    currency varchar(3) not null,
    status varchar(32) not null,
    provider_reference varchar(128) not null unique,
    callback_hash varchar(64),
    failure_reason varchar(256),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    confirmed_at timestamp with time zone,
    version bigint not null default 0
);

create index idx_payment_instruction_claim
    on payment_instruction(claim_reference, created_at);
