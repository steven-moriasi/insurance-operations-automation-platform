# Service Recovery

Use this runbook when a service fails readiness checks or the API error-rate alert fires.

## Triage

1. Identify the affected service and first failing trace.
2. Confirm whether PostgreSQL, MySQL, Temporal, Keycloak, object storage, or the integration target is unavailable.
3. Inspect deployment events, recent configuration changes, connection-pool saturation, and JVM memory before restarting anything.
4. Stop state-changing traffic if authorization, database integrity, or payment reconciliation is uncertain.

## Recovery

1. Restore the failed dependency or roll back the last deployment.
2. Restart stateless Spring Boot instances gradually; do not terminate all workflow workers together.
3. Confirm liveness and readiness probes, then verify a read-only API request.
4. Verify pending Temporal workflows, outbox records, automation leases, and payment instructions before restoring normal traffic.
5. Record the trace identifier, impacted business references, operator, action, and recovery time.

## Exit criteria

- all service probes are healthy;
- error rate and latency return to baseline;
- no payment or decision was applied twice;
- expired work leases are recoverable;
- in-flight workflows are queryable and continue from their persisted state.
