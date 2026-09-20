# Threat Model

## Scope

This threat model covers the reference platform, its browser application, APIs, workers, databases, object storage, event broker, workflow engine, and simulated external integrations. It does not certify a production deployment or replace an adopter's data-protection impact assessment and security review.

## Assets

- Customer identity and contact information.
- Policy and claim records.
- Evidence documents.
- Assessments and fraud referrals.
- Approval decisions and authority limits.
- Settlement instructions and reconciliation records.
- Authentication sessions and service credentials.
- Workflow histories, audit records, and operational telemetry.

## Trust Boundaries

1. Public browser to web BFF.
2. BFF to internal services.
3. Services to databases, object storage, broker, and Temporal.
4. Integration service to legacy, payment, messaging, and RPA systems.
5. Build pipeline to registries and deployment platforms.
6. Operators to administrative and replay controls.

## Principal Threats and Controls

### Account takeover and session abuse

- OIDC authentication and HTTP-only secure cookies.
- Short idle and absolute session lifetimes.
- CSRF protection for state-changing browser requests.
- Login and sensitive-action rate limits.
- Step-up authentication boundary for high-risk actions.
- Session revocation and security-event audit.

### Broken object-level authorization

- Server-side ownership and role checks on every protected resource.
- Policy tests covering customer, branch, tenant, and staff access.
- Non-sequential public identifiers.
- Deny-by-default authorization.

### Privilege escalation and approval fraud

- Separate assessment, approval, and payment permissions.
- Configurable financial authority limits.
- Maker-checker rules.
- Immutable decision events containing actor, authority, reason, and correlation identifiers.
- No administrative API that silently rewrites historical decisions.

### Forged or replayed callbacks

- Provider-specific authentication or message signatures.
- Timestamp tolerance and nonce or event-identifier replay detection.
- Idempotent processing.
- Exact amount, currency, account, and case validation before settlement.
- Immutable raw callback fingerprints without sensitive payload logging.

### Duplicate and out-of-order events

- Transactional outbox.
- Consumer idempotency keys.
- Aggregate sequence validation where ordering matters.
- Bounded retry and operator-visible dead-letter handling.

### Malicious documents

- File type, size, extension, and content checks.
- Malware-scanning boundary before documents are available to staff.
- Random object keys and private buckets.
- Short-lived authorized download links.
- Browser content-disposition controls.
- No document content in logs or traces.

### RPA compromise

- Robots receive narrowly scoped work items.
- Short-lived machine credentials.
- Network restrictions and separate robot identity.
- Callback authentication.
- No robot authority to approve claims or release payments.
- Complete item state and outcome audit.

### Workflow manipulation

- Only authorized services can start or signal workflows.
- Stable mapping between case and workflow identifiers.
- Signal payload validation and deduplication.
- Workflow versioning for in-flight executions.
- Administrative termination and replay actions require elevated authorization and reason capture.

### Sensitive-data leakage

- Structured logging allowlists.
- Telemetry redaction.
- No secrets, tokens, documents, or full personal identifiers in logs.
- Encryption in transit.
- Platform-managed encryption at rest in target environments.
- Documented retention and deletion extension points.

### Supply-chain compromise

- Pinned build actions and container images.
- Dependency and container scanning.
- Software bill of materials.
- Signed provenance for released images.
- Protected deployment environments.
- No secret-bearing build arguments or committed credentials.

### Denial of service and resource exhaustion

- Request limits and bounded uploads.
- Rate limits at public endpoints.
- Queue depth, age, and saturation metrics.
- Worker concurrency limits.
- Database timeouts and connection-pool limits.
- Autoscaling and disruption budgets in target manifests.

## Residual Risks

The reference environment uses synthetic identity, payment, legacy, notification, and RPA integrations. Production adopters must validate provider authentication, tenancy design, data residency, retention, fraud controls, backup restoration, incident response, privileged access, and regulatory obligations in their own environment.
