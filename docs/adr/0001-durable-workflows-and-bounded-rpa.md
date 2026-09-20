# ADR 0001: Use Durable Workflows and Keep RPA Bounded

- Status: Accepted
- Date: 2026-09-20

## Context

Insurance operations combine long-running business processes, human decisions, legacy applications, documents, deadlines, and financial side effects. A portal database can store status, but application code must then rebuild timers, retries, recovery, and orchestration. An RPA tool can execute user-interface steps, but it is a poor authority for business state.

## Decision

Temporal owns durable process execution. Spring Boot services own authoritative business data and authorization. RPA workers receive bounded work items through a versioned queue contract and return structured results.

Workflow code:

- coordinates activities, timers, signals, and compensation;
- remains deterministic;
- records stable business identifiers;
- does not perform database or network input/output directly.

RPA adapters:

- use idempotency keys;
- cannot approve claims or release payments;
- report progress and outcomes through authenticated callbacks;
- can be replaced without changing the case model or workflow contract.

## Consequences

The platform gains durable timers, workflow histories, controlled retry, and recovery after worker restarts. Business state remains queryable independently of the workflow engine. Teams must operate Temporal and design workflow versioning. Legacy automation remains possible, but robots do not become an invisible system of record.
