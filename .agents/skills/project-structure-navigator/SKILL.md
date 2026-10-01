---
name: project-structure-navigator
description: >-
  Complete architecture map, directory layout, and file placement rules for the Unikly platform.
  Use when onboarding new contributors, deciding where to place new backend/frontend files,
  or navigating domain boundaries.
---

# Unikly Project Structure & Placement Rules

This skill provides an effortless mental model of the Unikly codebase. It defines where every file lives, how domains interact, and the strict placement rules for adding new features.

---

## 1. High-Level Repository Architecture

```text
unikly/
├── .agents/skills/         # Modular AI agent skills (progressive disclosure runbooks)
├── backend/                # Spring Boot 4.x / Java 25 Modular Monolith
│   ├── src/main/java/com/unikly/store/
│   │   ├── catalog/        # Product listing, stock inventory, seller catalog
│   │   ├── identity/       # Users, auth, password hashing, session/CSRF, RBAC
│   │   ├── orders/         # Orders, line items, delivery methods, fulfillment
│   │   ├── profile/        # Buyer delivery address & contact details
│   │   └── platform/       # Cross-cutting security & HTTP configuration
│   └── src/main/resources/db/migration/ # Flyway migrations (V1__ to V<N>__)
├── frontend/               # Angular 22.2 Standalone Client (Node 24 Alpine)
│   ├── public/             # Static assets (SVGs, placeholders, favicons)
│   └── src/app/
│       ├── core/           # Singleton services, state management, HTTP APIs
│       │   ├── cart/       # Client-side basket & local storage persistence
│       │   ├── identity/   # Auth state, login/logout, profile service
│       │   └── orders/     # Order placement, cancellation, seller tracking
│       └── features/       # Screen-level views and user flows
│           ├── account/    # Buyer account settings & order history
│           ├── home/       # Catalog grid, product detail, basket/checkout, seller portal
│           └── identity/   # Login & registration forms
├── docs/                   # Product SRS (v2/v3), architecture decisions, diagrams
├── AGENTS.md               # Foundational agent rules & engineering constraints
├── CHANGELOG.md            # Verified increment & bugfix journal
├── docker-compose.yml      # Local multi-container topology (Postgres, API, Web)
└── UNIKLY_BUILD_AND_ONBOARDING_GUIDE.md # SRS implementation roadmap
```

---

## 2. "Where Does My Code Go?" — Placement Cheat Sheet

| I want to add or modify... | Target Location | Architectural Rule |
| :--- | :--- | :--- |
| **New Database Table / Column** | `backend/src/main/resources/db/migration/V<N>__<name>.sql` | Never edit old migrations. Always write an immutable forward `V<N+1>` migration. |
| **Database Entity / Enum** | `backend/.../<domain>/domain/` | Keep entities rich with invariants. Use `BigDecimal` for money, `TIMESTAMPTZ` for dates. |
| **Spring Data Repository** | `backend/.../<domain>/persistence/` | Extend `JpaRepository`. Never expose entities directly outside the domain service. |
| **Business Logic / Workflow** | `backend/.../<domain>/application/` | Mark service with `@Transactional`. Use `@Transactional(readOnly = true)` for queries. |
| **Request / Response DTO** | `backend/.../<domain>/application/` | Define as immutable Java **records** with `@Valid` constraints (e.g. `OrderRequests.java`). |
| **REST Controller / Endpoint** | `backend/.../<domain>/api/` | Map under `/api/<domain>`. Return records or standard status codes (`200`, `201`, `204`). |
| **Global State / Shared API Client** | `frontend/src/app/core/<domain>/` | Use `providedIn: 'root'`. Expose reactive state via Angular Signals (`signal()`, `computed()`). |
| **New Page / Route Component** | `frontend/src/app/features/<feature>/pages/<page-name>/` | Must be a standalone component with modern control flow (`@if`, `@for (...; track ...)`). |
| **Static Icon / Image Asset** | `frontend/public/` | Reference with root path `/asset-name.svg`. Attach `(error)` fallback handlers to `<img>`. |

---

## 3. Backend Domain Boundaries

Code is organized into 4 primary business domains:

1. **`identity`**:
   - Manages `StoreUser` accounts, roles (`BUYER`, `SELLER`, `ADMIN`), and passwords via BCrypt.
   - Manages authentication sessions and CSRF token verification.
2. **`catalog`**:
   - Manages `CatalogProduct` entities, categories, images, and inventory stock quantities.
   - Restricts modifications: sellers can only edit/delete products where `seller_id == user.id`.
3. **`orders`**:
   - Manages `CustomerOrder` and `CustomerOrderItem` entities.
   - Handles delivery options (`STANDARD`, `EXPRESS`), authoritative shipping fee calculation, and order state transitions (`PLACED`, `SHIPPED`, `DELIVERED`, `CANCELED`).
4. **`profile`**:
   - Manages `CustomerProfile` records (name, phone, delivery address lines, city, postal code).
   - Auto-populates buyer checkout fields.

> [!IMPORTANT]
> **Cross-Domain Rule**: Domains interact through public service methods in `application/`. Never perform direct entity-to-entity mutations across domain boundaries.

---

## 4. Frontend Layering Rules

1. **`core/`**:
   - Singleton services that manage communication with the Spring Boot API.
   - Must contain no UI templates or page styling.
   - Handles CSRF token forwarding, credentials inclusion, and local storage synchronization.
2. **`features/`**:
   - Self-contained feature components (`.ts`, `.html`, `.css`).
   - Use constructor-less dependency injection via `inject(Service)`.
   - Never use `NgModules`; directly declare dependencies in the component's `imports: [...]`.
   - Always ensure external images have `(error)="$any($event.target).src = '/product-placeholder.svg'"`.
