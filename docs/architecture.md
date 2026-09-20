# Insurance Operations Automation Platform

## What We Are Building and Why

The Insurance Operations Automation Platform is an open reference implementation for Kenyan insurance companies that want to digitize document-heavy, human-in-the-loop operations without losing control, accountability, or the ability to recover from failures.

The system demonstrates how an insurer can combine customer self-service, staff work queues, durable workflow orchestration, integration with legacy systems, robotic-process-automation boundaries, and production operations in one coherent platform. It is deliberately built as inspectable source code rather than a slideware architecture.

This repository uses synthetic people, policies, claims, payments, documents, rules, and integrations. It is a starting point for engineering teams, not deployable insurance software and not a representation of CIC Group or any other insurer's proprietary systems, underwriting rules, claims practices, security controls, or customer data.

## Why This Platform Exists

Insurance operations frequently cross channels, departments, and systems. A single motor claim can require policy verification, evidence collection, assessment, fraud review, approval, payment, customer communication, and regulatory recordkeeping. Policy renewals and intermediary onboarding have similar characteristics:

- work can remain open for days or weeks;
- automated steps are interleaved with human decisions;
- external systems can be unavailable or return duplicate responses;
- financial and customer-impacting actions require clear authority boundaries;
- staff need a complete case history rather than fragmented status updates;
- process owners need evidence of delays, retries, exceptions, and manual effort.

A conventional CRUD portal can record data, but it does not by itself provide durable orchestration, time-based escalation, safe retry, compensation, or operational visibility. A collection of unattended RPA scripts can automate individual screens, but it can hide business state inside robots and become difficult to audit or recover.

We are building a case-management and workflow platform in which:

1. Spring Boot services own business state and authorization.
2. Temporal owns durable process execution, timers, signals, and recovery.
3. React gives customers and staff one role-aware web application.
4. Python and RPA adapters automate bounded tasks without becoming the system of record.
5. Events and explicit integration contracts connect services and legacy systems.
6. OpenTelemetry makes technical and business process behavior observable.
7. OpenShift and AWS definitions show how the system can be operated beyond a laptop.

## Who This Reference Is For

The repository is intended for:

- insurers evaluating workflow and case-management architecture;
- engineering teams modernizing legacy policy or claims processes;
- automation teams moving from isolated scripts to governed services;
- architects comparing synchronous APIs, event-driven integration, RPA, and durable workflows;
- developers learning production-oriented React, Java, Spring Boot, Python, Temporal, PostgreSQL, MySQL, AWS, and OpenShift patterns.

Adopters are expected to replace synthetic rules and integrations, complete their own security and privacy reviews, validate all legal and regulatory obligations, and perform production capacity and resilience testing.

## The Insurance Journeys

### Motor claim handling

Motor claims are the deepest reference journey because they exercise customer intake, documents, long-running work, financial controls, and external integration.

```text
First notification of loss
        |
        v
Policy and coverage verification
        |
        v
Evidence checklist and document collection
        |
        v
Claims officer triage
        |
        +------> Fraud review when referred
        |
        v
Assessment and loss estimate
        |
        v
Authority-based approval
        |
        v
Settlement instruction and reconciliation
        |
        v
Customer notification and case closure
```

The workflow supports missing evidence, policy-system outages, duplicate callbacks, assessor delays, manual referral, rejected settlements, payment timeouts, and controlled replay.

### Policy renewal and endorsement

The platform identifies policies approaching expiry, opens renewal cases, schedules communications, records customer decisions, coordinates payment, and applies maker-checker controls to material changes.

This journey proves that timers, tasks, notifications, approvals, audit records, and integration primitives are reusable outside claims.

### Broker and agent onboarding

The onboarding journey collects evidence, executes automated validation activities, routes exceptions to reviewers, enforces segregation of duties, and measures elapsed time and manual effort.

This journey demonstrates that the platform can coordinate an operational process that is not tied to a policy or claim transaction.

## One Product, Multiple Role-Specific Workspaces

Users access one authenticated React application. The browser communicates only with the web backend-for-frontend; internal services are not exposed as independent public applications.

Initial roles are:

| Role | Primary responsibilities |
|---|---|
| `customer` | Submit and track personal claims and requested evidence |
| `contact-centre-agent` | Register cases on behalf of customers |
| `claims-officer` | Triage cases, manage evidence, and make routine decisions |
| `assessor` | Complete assigned vehicle or loss assessments |
| `fraud-reviewer` | Investigate referred cases and record outcomes |
| `approver` | Authorize decisions within assigned financial limits |
| `finance-operator` | Release and reconcile settlement instructions |
| `process-owner` | Inspect service levels, bottlenecks, and automation outcomes |
| `platform-admin` | Manage access, integrations, reference data, and configuration |

Authorization is enforced by server-side policy. Hiding a screen or button is never treated as sufficient access control.

## System Context

```text
Customers and staff
        |
        v
React web application
        |
        v
Spring Boot web BFF
        |
        +-------------------+---------------------+
        |                   |                     |
        v                   v                     v
Case service         Integration service    Temporal service
        |                   |                     |
        v                   v                     v
PostgreSQL       Kafka / adapters / outbox   Workflow workers
                            |
             +--------------+----------------+
             |              |                |
             v              v                v
       Legacy policy    Python worker    RPA boundary
          simulator       and OCR         and simulator
             |
             v
           MySQL
```

S3-compatible object storage keeps evidence files outside transactional database rows. PostgreSQL stores platform records. MySQL represents a realistic legacy-system boundary rather than a second database introduced without a business reason.

## Deployable Components

### Web BFF

The BFF is the browser-facing trust boundary. It handles OIDC login, secure session cookies, CSRF protection, role-aware aggregation, request validation, rate limits, correlation identifiers, and response security headers.

### Case service

The case service owns claims, policies imported into the automation context, parties, tasks, evidence metadata, assessments, decisions, settlement records, and immutable audit events.

### Integration service

The integration service isolates external and legacy protocols from core business code. It owns adapters for policy verification, payments, customer messaging, RPA queues, callbacks, and event publication.

### Temporal workers

Temporal workers implement durable business processes. Workflow code coordinates activities but does not perform database or network input/output directly. Activities are idempotent where retry can occur.

### Python automation worker

The Python worker performs bounded document classification, file validation, and scripted legacy tasks. Its results are advisory or activity-scoped; it does not become the authority for claims, approvals, or payments.

## Workflow Design

Temporal is used where the process must survive time, failure, and human delay. Workflows support:

- durable timers and service-level deadlines;
- explicit human-task signals;
- bounded retries and non-retryable business failures;
- escalation when evidence or decisions are overdue;
- cancellation and compensation;
- deterministic replay;
- versioning for workflows already in progress;
- search attributes for operational discovery;
- complete workflow histories for diagnosis.

Short synchronous reads and writes remain ordinary Spring Boot API operations. Temporal is not used merely to replace a method call.

## Event and Integration Model

The platform uses versioned event envelopes carrying:

- event identifier;
- event type and schema version;
- aggregate identifier and sequence;
- correlation and causation identifiers;
- occurred-at timestamp;
- tenant or operating-company context;
- trace context;
- payload.

Database changes and event publication use a transactional outbox. Consumers maintain idempotency records and reject invalid ordering where ordering is required. Failed messages move through bounded retry into an operator-visible dead-letter path.

Incoming callbacks require authentication or signatures where the simulated provider supports them. Replay detection, duplicate handling, amount and currency validation, and immutable reconciliation records protect financial workflows.

## RPA and Low-Code Boundaries

RPA is treated as an integration mechanism, not the business-process authority.

The platform exposes a UiPath-compatible work-queue contract:

1. The integration service creates a work item with a stable idempotency key.
2. A robot or local simulator claims the item.
3. The robot reports structured progress and a final result.
4. The service verifies the callback and records the result.
5. Temporal advances or escalates the business workflow.

The local environment uses a deterministic simulator because the repository does not bundle a UiPath license or claim a live insurer desktop integration. The same contract can be implemented by UiPath, another RPA product, or a custom adapter.

Low-code tools may own departmental forms or bounded approval interfaces, but core authorization, financial decisions, workflow state, and audit history remain in governed services.

## Data Ownership

| Data | Authority |
|---|---|
| Case, task, decision, assessment, settlement, audit | PostgreSQL case service |
| Workflow execution, timers, and signals | Temporal |
| Synthetic legacy policies and billing records | MySQL legacy simulator |
| Document binaries | S3-compatible object storage |
| Document metadata and access decisions | Case service |
| Integration delivery and idempotency | Integration service |
| Search and operational projections | Rebuildable projections |

Cross-service database access is prohibited. Services communicate through APIs and events.

## Security Model

The platform is designed around:

- OIDC authentication and short-lived server-side sessions;
- least-privilege role and permission policies;
- ownership checks for customer records;
- maker-checker and financial authority limits;
- segregation between claim assessment, approval, and payment;
- immutable decision and access audit events;
- encryption in transit and configurable encryption at rest;
- secrets supplied through platform secret stores;
- document type, size, malware, and content validation boundaries;
- redaction of personal and financial data from logs and telemetry;
- network policies restricting service-to-service traffic;
- non-root containers with read-only filesystems where practical;
- dependency, container, source, and infrastructure scanning.

This reference does not certify compliance. A production adopter must complete data-protection impact assessment, retention design, records-management review, fraud controls, legal review, regulatory review, and independent security testing for its own context.

## Observability and Process Evidence

OpenTelemetry carries trace context across browser requests, Spring services, Kafka messages, Temporal activities, and Python jobs. Micrometer publishes technical and business metrics.

Technical signals include:

- request latency and error rates;
- database and external dependency latency;
- consumer lag;
- workflow-task and activity failures;
- retry and dead-letter counts;
- queue age;
- worker saturation;
- deployment health.

Process signals include:

- claim cycle time;
- time spent in each process stage;
- straight-through-processing rate;
- manual-touch rate;
- service-level breaches;
- evidence turnaround time;
- referral and rejection rates;
- settlement reconciliation failures;
- automation completion and exception rates.

Dashboards separate measured local evidence from production targets. No production service-level claim is made without a real deployment and operating history.

## Local and Target Environments

### Local environment

One documented startup command launches the web application, services, workers, databases, object storage, Temporal, event broker, identity provider, and observability stack with synthetic seed data.

The local system is expected to exercise complete workflows without proprietary credentials.

### OpenShift target

OpenShift deployment definitions include:

- routes and service exposure;
- readiness, liveness, and startup probes;
- requests, limits, autoscaling, and disruption budgets;
- service accounts and workload identity boundaries;
- network policies;
- non-root security contexts;
- immutable image references;
- configuration and secret references;
- rolling deployment and rollback guidance.

### AWS target

Infrastructure as code defines a reviewable target using managed equivalents for networking, PostgreSQL, object storage, secrets, container images, identity, and observability integration.

The repository distinguishes validated infrastructure definitions from proof of a live cloud deployment.

## Testing Strategy

The system uses:

- JUnit 5 for domain and service behavior;
- Spring Boot integration tests;
- Testcontainers for PostgreSQL, MySQL, Kafka, and integration boundaries;
- Temporal workflow tests with time skipping;
- WireMock for external APIs;
- contract tests for adapter compatibility;
- ArchUnit for module and dependency boundaries;
- Python unit and integration tests;
- React unit and component tests;
- Playwright for authenticated golden paths;
- load tests for intake and staff queues;
- executable failure and recovery drills.

Important recovery scenarios include policy-system outages, duplicate callbacks, delayed human responses, worker restarts, broker interruption, poison messages, document-processing failures, payment timeouts, and projection rebuilds.

## Delivery Principles

The project follows these principles:

1. Business state is explicit and queryable.
2. Automation is observable and interruptible.
3. Human decisions are first-class workflow events.
4. Every retryable side effect is idempotent.
5. Financial actions require reconciliation, not optimistic success.
6. RPA adapters are replaceable.
7. External failures do not corrupt authoritative state.
8. Security is enforced at APIs and data boundaries.
9. Infrastructure definitions are tested but not presented as deployment proof.
10. Documentation states what exists, what is simulated, and what remains unverified.

## What This Reference Does Not Claim

This repository does not claim:

- to implement any named insurer's internal process;
- to encode production underwriting, fraud, claims, pricing, or approval rules;
- to satisfy Kenyan or regional insurance regulation by itself;
- to contain real customer or policy data;
- to connect to a live core insurance, payment, messaging, or RPA platform unless explicitly documented;
- to prove cloud-scale performance through local benchmarks;
- to be suitable for production without insurer-specific engineering, assurance, and governance.

Its value is a concrete, testable starting point from which an insurer can evaluate architecture, adapt workflows, replace simulators with governed integrations, and build evidence appropriate to its own operations.
