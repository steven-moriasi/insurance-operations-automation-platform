# Security Control Evidence

This register maps implemented controls to inspectable evidence. It is not a compliance certification.

| Boundary | Implemented control | Evidence |
|---|---|---|
| Browser session | OIDC authorization-code login, HTTP-only session cookie, CSRF token and logout protection | `web-bff` security configuration and session tests |
| Browser response | Content Security Policy, clickjacking denial, no-referrer policy, Spring Security defaults | `SecurityConfig` |
| API authorization | Deny-by-default route policy plus method-level role checks | service security configurations and controller tests |
| Service identity | JWT resource servers and workflow client-credentials grant | service application configuration |
| Claims decisions | server-side role and authority-limit enforcement, immutable audit records | case service decision model and tests |
| Payments | signed callbacks, exact amount/currency reconciliation, duplicate and conflict protection | payment service and tests |
| Documents | private object keys, short-lived URLs, allowed types, digest/size checks, malware-scan boundary | document service, Python worker, and tests |
| Automation workers | narrow work types, expiring leases, token-bound completion, bounded retry, dead-letter state | automation queue and tests |
| Durable workflows | authorized start/signal APIs, deterministic timers, cancellation, exception routes | Temporal workflows and tests |
| Secrets | environment and platform-secret references; no production credentials in source | service configuration and deployment definitions |
| Telemetry | trace identifiers in logs, OTLP export, bounded metric labels, no document payload metrics | service configuration and observability definitions |

Production adopters must add tenancy rules, privileged-access governance, key rotation, security monitoring, penetration testing, data-retention controls, and provider-specific callback authentication.
