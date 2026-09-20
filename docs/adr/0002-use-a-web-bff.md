# ADR 0002: Expose One Web Backend-for-Frontend

- Status: Accepted
- Date: 2026-09-20

## Context

The platform has customer and staff journeys backed by several internal services. Exposing every service to the browser would spread authentication, cross-origin configuration, aggregation, and public API concerns across the system.

## Decision

The React application communicates with a Spring Boot backend-for-frontend. The BFF owns browser sessions, CSRF protection, rate limiting, response security headers, role-aware aggregation, and translation from browser-oriented requests to internal service contracts.

Internal services are private and authenticate service-to-service traffic separately. Server-side authorization remains mandatory in every service that owns protected data or actions.

## Consequences

The browser has one secure origin and a stable interface. Internal service boundaries can evolve without exposing them directly. The BFF must remain focused on presentation and trust-boundary responsibilities rather than accumulating domain ownership.
