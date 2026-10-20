---
name: spring-boot-developer
description: >-
  Develop and review the Unikly Spring Boot 4.1 modular monolith in Java 25. Use for backend modules, REST APIs, services, persistence, security, and exception handling.
---

# Spring Boot developer

Unikly is a Spring Boot modular monolith. Confirm existing code and dependency conventions before adding a type or abstraction. Use only the layers required by the feature.

## Module boundaries and Java design

- Business modules are identity, catalog, cart, orders, profile, and reviews; platform contains shared HTTP and security configuration.
- Keep entities and repositories inside their owning module. Cross-module business work goes through the owning module's public application service.
- Keep controllers thin. Put use cases and transaction boundaries in application services.
- Use immutable request and response records. Validate untrusted input at the API boundary and retain domain invariants in the domain or application layer.
- Prefer clear, immutable Java 25 code and explicit types at public boundaries. Use Optional as a return value for absence, not as an entity field or method parameter. Avoid clever stream chains where a loop is clearer.
- Do not expose JPA entities as API payloads or serialize lazy relationships.
- Use read-only transactions for query operations where appropriate.

## Persistence and query behavior

- Read ../postgres-flyway-architect/SKILL.md before changing persistence or schema.
- Check query shape, fetch behavior, pagination, and indexes together. Avoid N+1 queries and unbounded collection responses.
- Do not access another module's repository or mutate its entities directly.
- Keep database constraints authoritative for relational invariants and add a forward Flyway migration when the schema changes.

## API, security, and domain behavior

- Use explicit request validation and appropriate HTTP status codes.
- Authentication uses Argon2 password hashes and server-side sessions; it does not use bearer tokens in browser storage.
- State-changing requests require the session's CSRF token. Login rotates the session ID, so clients must fetch a fresh token after login.
- Read docs/authorization-matrix.md for current route permissions. Verify controller mappings, security configuration, service ownership checks, and focused tests together.
- Never rely on Angular guards or client-supplied identity, role, ownership, price, inventory, or order totals for authorization or business correctness.
- Scope exception advice to the responsible controller or module. Do not turn unrelated integrity errors into misleading responses or expose stack traces, SQL details, secrets, or private data.
- Use BigDecimal for all monetary values. Calculate prices, delivery fees, and order totals on the server.
- Model state transitions, idempotency, and resource ownership in the application or domain layer.

## Verification

For backend behavior changes, add or update focused tests for affected behavior and invariants, then run them in the Java 25 Docker toolchain. For API, security, persistence, order, or cross-layer changes, run the applicable local Compose smoke flow when requested by project workflow. See ../docker-toolchain-isolated/SKILL.md and ../e2e-smoke-testing/SKILL.md.
