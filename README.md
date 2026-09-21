# Insurance Operations Automation Platform

An open, synthetic reference implementation for Kenyan insurance companies exploring governed process automation, durable workflows, legacy integration, and human-in-the-loop case management.

The platform is being built as one authenticated React application backed by Java and Spring Boot services, Temporal workflows, Python automation, PostgreSQL, a synthetic MySQL policy system, event-driven integration, and production-oriented OpenShift and AWS definitions.

## Why it exists

Insurance processes such as claims, renewals, and intermediary onboarding span people, documents, systems, deadlines, and financial controls. This repository demonstrates how to make that work explicit, recoverable, observable, and auditable without hiding the business process inside a portal or an RPA script.

Read [What We Are Building and Why](docs/architecture.md) for the authoritative product and architecture description.

## Reference journeys

- Motor claim intake through settlement reconciliation.
- Policy renewal and controlled endorsements.
- Broker and agent onboarding.

Motor claims provide the deepest implementation. The other journeys prove that the workflow and case-management primitives are reusable.

## Run the complete local system

Docker Compose starts the React application, BFF, three internal Spring Boot services, Python automation worker, PostgreSQL, synthetic MySQL policy store, Keycloak, Temporal, MinIO, OpenTelemetry Collector, Jaeger, Prometheus, and Grafana:

```bash
docker compose up --build
```

Open `http://localhost:3000` and sign in with the synthetic local user `operations.owner` / `local-operations-only`. Temporal UI is exposed on port `8089`, Jaeger on `16686`, Prometheus on `9090`, and Grafana on `3001`. All credentials in `compose.yaml` are local-only examples and must never be reused in a shared or deployed environment.

See [Delivery and Deployment Evidence](docs/delivery.md) for image, OpenShift, AWS, promotion, and rollback contracts.

## Truth boundary

This project:

- uses only synthetic data and simulated integrations;
- is not affiliated with CIC Group or another insurer;
- does not reproduce proprietary insurer processes or rules;
- is not a compliance certification or production deployment;
- requires insurer-specific legal, security, privacy, actuarial, and operational review before adoption.

## License

Licensed under the [Apache License 2.0](LICENSE) so teams can inspect, adapt, and build on the reference implementation subject to its terms.
