# Unikly — AI Agent & Developer Rules (`AGENTS.md`)

This file contains foundational project rules and architectural constraints for AI agents and developers working on the **Unikly** marketplace platform.

---

## 1. Core Engineering Principles

1. **"Build Small and Prove It Before Building Big"**:
   - Follow the **Unikly Build & Onboarding Guide** and SRS.
   - Do not jump ahead to premature architectural complexity (e.g. distributed microservices, external payment gateways, live carrier tracking integrations) before earlier foundations are complete and verified.
   - All complex increments (payment gateways, live carrier APIs, third-party email servers) are deferred until last.

2. **Strict Toolchain Isolation (Host vs. Container)**:
   - **Host Machine**: Uses OpenJDK 17 and Node 22.
   - **Target Project**: Requires **Eclipse Temurin Java 25** and **Node v24.21.0 Alpine**.
   - **MANDATORY**: Never run `./mvnw test` or `npm run build` directly on the host machine.
   - **Always run Maven backend tests via Docker**:
     ```bash
     docker run --rm -v "${HOME}/.m2:/root/.m2" -v "$(pwd)/backend:/workspace" -w /workspace unikly-api-test mvn test
     ```
   - **Always run Angular builds via Docker**:
     ```bash
     docker build -t unikly-web-test ./frontend
     ```

3. **Mandatory Live Verification**:
   - Never conclude that a task is finished based purely on unit tests or compilation.
   - Run end-to-end smoke tests against the active Docker Compose stack (`http://localhost:8080` API, `http://localhost:4200` Web, PostgreSQL) to verify full request lifecycles.

---

## 2. Backend Architecture (Spring Boot & Java 25)

1. **Modular Monolith by Domain**:
   - Organize code into clean domain packages under `com.unikly.store.*` (`identity`, `catalog`, `orders`, `profile`).
   - Separate layers: `domain/`, `persistence/`, `application/`, `api/`.
2. **DTOs as Records**:
   - Use immutable Java records for request payloads and response views. Validate input with `@Valid` and Jakarta constraints.
3. **Transaction Demarcation**:
   - Annotate service classes with `@Transactional`.
   - Use `@Transactional(readOnly = true)` for query methods.
4. **Scoped Exception Handlers**:
   - Always scope `@RestControllerAdvice` to specific controllers or packages (`@RestControllerAdvice(assignableTypes = AuthController.class)`).
   - Never swallow or mask general database integrity exceptions.
5. **Authoritative Financial Calculations**:
   - Never use `double` or `float` for prices or fees. Use `BigDecimal` with 2 decimal places and `RoundingMode.HALF_UP`.
   - Delivery fees and order totals must be computed authoritatively on the backend.

---

## 3. Frontend Architecture (Angular 22.x & TypeScript)

1. **Standalone Components**:
   - All components, pipes, and directives must be standalone. No `NgModules`.
2. **Signal-Based Reactivity**:
   - Use `signal()`, `computed()`, and `effect()`. Avoid manual subscriptions and legacy `RxJS` subject plumbing where signals fit.
3. **Modern Control Flow**:
   - Use `@if`, `@for (...; track ...)`, and `@switch`. Do not use `*ngIf` or `*ngFor`.
4. **Constructor-less Injection**:
   - Inject dependencies using `inject(Service)`.
5. **Defensive Templates & Accessibility**:
   - Always attach `(error)="$any($event.target).src = '/product-placeholder.svg'"` and `loading="lazy"` to external images.
   - Maintain WCAG 2.1 AA compliance: semantic elements, accessible labels (`[attr.aria-label]`), and `role="status"` for dynamic updates.

---

## 4. Database & Migrations (PostgreSQL 18 & Flyway)

1. **Immutable Migrations**:
   - Place migrations in `backend/src/main/resources/db/migration/` as `V<N>__<description>.sql`.
   - Never edit or delete an existing migration file. Always create a new forward migration.
2. **Schema Invariants**:
   - Ensure foreign keys and relational constraints (such as `seller_id NOT NULL` on order items) are consistently populated in seed data and migrations.

---

## 5. Documentation & Communication

- Maintain [`CHANGELOG.md`](file:///home/aboubakar-garba/Documents/Projects/BrandNew/unikly/CHANGELOG.md) with concise bullet points for every increment and bugfix.
- Preserve existing comments and docstrings.
- Always provide clickable GitHub-style markdown links with the `file://` scheme for modified files.
