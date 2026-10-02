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

## 2. Project Structure & Code Placement Guide

Anyone joining or contributing to Unikly must follow this clean layout:

```text
unikly/
├── .agents/skills/         # Specialized AI agent skills and runbooks
├── backend/                # Spring Boot 4.x / Java 25 Modular Monolith
│   ├── src/main/java/com/unikly/store/
│   │   ├── catalog/        # Product catalog, stock, seller inventory
│   │   ├── identity/       # Users, auth, password hashing, session/CSRF, RBAC
│   │   ├── orders/         # Orders, line items, delivery options, fulfillment
│   │   ├── profile/        # Customer delivery address & contact details
│   │   └── platform/       # Cross-cutting security & HTTP configuration
│   └── src/main/resources/db/migration/ # Flyway SQL migrations (V1__ to V<N>__)
├── frontend/               # Angular 22.2 Standalone Client (Node 24 Alpine)
│   ├── public/             # Static assets (SVGs, logos, placeholders)
│   └── src/app/
│       ├── core/           # Singleton services, state management, HTTP APIs
│       └── features/       # Screen-level views (account, home, identity)
├── docs/                   # Product SRS (v2/v3) and architecture specs
├── AGENTS.md               # Foundational agent rules & engineering constraints
├── CHANGELOG.md            # Verified increment & bugfix journal
├── docker-compose.yml      # Local multi-container topology (Postgres, API, Web)
└── UNIKLY_BUILD_AND_ONBOARDING_GUIDE.md # SRS implementation roadmap
```

### Where Does My Code Go?

| I need to add / modify... | Target Location | Placement Rule |
| :--- | :--- | :--- |
| **New DB Table / Column** | `backend/src/main/resources/db/migration/` | Add a new immutable forward migration `V<N+1>__<name>.sql`. |
| **JPA Entity / Enum** | `backend/.../<domain>/domain/` | Keep entities rich with domain validation and invariants. |
| **Data Repository** | `backend/.../<domain>/persistence/` | Extend `JpaRepository<Entity, ID>`. |
| **Business Service / Flow** | `backend/.../<domain>/application/` | Annotate with `@Transactional`. Use `readOnly = true` for queries. |
| **Request / Response DTO** | `backend/.../<domain>/application/` | Define as immutable Java **records** with `@Valid` constraints. |
| **REST Controller** | `backend/.../<domain>/api/` | Map under `/api/<domain>`. Return records or standard status codes. |
| **Shared State / API Service** | `frontend/src/app/core/<domain>/` | Use `providedIn: 'root'`. Expose reactive state via Signals. |
| **Page / Route View** | `frontend/src/app/features/<domain>/pages/`| Create standalone component with `@if` and `@for (...; track ...)`. |
| **Static SVG / Image** | `frontend/public/` | Place SVG here; always attach `(error)` fallback handlers to `<img>`. |

---

## 3. Backend Architecture (Spring Boot & Java 25)

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

## 4. Frontend Architecture (Angular 22.x & TypeScript)

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
6. **Strictly Vector Icons (No Emojis)**:
   - Always use crisp, accessible inline SVG vector icons instead of unicode emojis (e.g. never use 🗑, 🚚, 🛒, ✓).
   - Ensure SVGs specify accessible attributes (`aria-hidden="true"` or explicit `[attr.aria-label]`), standard sizing (`width`, `height`), and inherit theme colors (`stroke="currentColor"` or `fill="currentColor"`).

---

## 5. Database & Migrations (PostgreSQL 18 & Flyway)

1. **Immutable Migrations**:
   - Place migrations in `backend/src/main/resources/db/migration/` as `V<N>__<description>.sql`.
   - Never edit or delete an existing migration file. Always create a new forward migration.
2. **Schema Invariants**:
   - Ensure foreign keys and relational constraints (such as `seller_id NOT NULL` on order items) are consistently populated in seed data and migrations.

---

## 6. Documentation & Communication

- Maintain [`CHANGELOG.md`](CHANGELOG.md) with structured, domain-categorized summaries for every increment and bugfix.
- In repository documentation and committed markdown files, always use clean relative paths—never leak absolute local filesystem paths or usernames.
- Preserve existing comments and docstrings.
- In interactive chat responses, provide clickable GitHub-style markdown links with the `file://` scheme for modified files for direct local editor navigation.
