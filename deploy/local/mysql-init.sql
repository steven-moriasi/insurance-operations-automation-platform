CREATE TABLE legacy_policy (
    policy_number VARCHAR(64) PRIMARY KEY,
    product_code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    cover_start_date DATE NOT NULL,
    cover_end_date DATE NOT NULL,
    currency CHAR(3) NOT NULL
);

INSERT INTO legacy_policy (
    policy_number,
    product_code,
    status,
    cover_start_date,
    cover_end_date,
    currency
) VALUES (
    'SYN-MOTOR-001',
    'PRIVATE-MOTOR-COMPREHENSIVE',
    'ACTIVE',
    '2026-01-01',
    '2026-12-31',
    'KES'
);
