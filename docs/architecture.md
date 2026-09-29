# Application Architecture

## Backend: modular monolith

The API remains one Spring Boot deployable. Code is organized by business capability, with infrastructure concerns kept separate:

```text
backend/src/main/java/com/unikly/store/
  UniklyApplication.java
  identity/
    api/                    # Auth endpoints, request/response types, module error mapping
    domain/                 # StoreUser, StoreRole, StorePermission
    persistence/            # Identity repositories and persistence adapters
    security/               # UserDetails adapter for Spring Security
  platform/security/        # Cross-cutting HTTP security configuration
```

Only implemented modules should have source packages. Do not create empty placeholder packages. A module owns its domain model, application/use-case services, persistence adapters and API endpoints. Cross-module work goes through a public application service or explicit event, not another module's repositories or internal entities. Keep Spring configuration out of domain packages. `UniklyApplication` stays above the modules so component scanning covers the modular monolith.

When product features are implemented, add `catalog`, `cart`, `ordering`, and `reviews` modules using the same API/application/domain/persistence layering where each layer is needed; do not create empty placeholder packages in advance.

The test tree mirrors production modules under `backend/src/test/java/com/unikly/store/identity`. Database migrations are versioned under `backend/src/main/resources/db/migration`.

## Frontend: feature areas

```text
frontend/src/
  main.ts
  styles.css
  app/
    app.ts                 # Composition shell only
    app.routes.ts          # Lazy feature route entry points
    app.config.ts          # Global providers/initializers
    core/                  # Cross-feature singleton infrastructure
      identity/            # Session state and route guards
    features/
      identity/            # Sign-in and registration workflows
        pages/auth-page/    # Colocated template and styles
        identity.routes.ts  # Feature-owned route definitions
        public-api.ts       # Only route surface consumed by app router
      account/             # Current user's account view
        pages/account-page/ # Colocated page, template, and styles
        account.routes.ts
        public-api.ts
      home/                # Public storefront landing and sample product browsing
    core/cart/             # Browser-local demo basket; replace with cart API in commerce phase
  public/
```

Feature routes and pages are lazy loaded. A feature owns its routes, pages, UI and feature-specific services; reusable global infrastructure belongs in `core`. Do not put business screens in the root app shell. Use `@core/*` and `@features/*` aliases for stable imports. Import across feature boundaries only through a feature's documented public entry point; never reach into another feature's page/component internals. If a feature grows, split it into `data-access`, `ui`, and `pages` within that feature rather than creating a global grab-bag folder.

## Dependency direction

- `app` composes `core` and lazy `features`.
- Features may use public APIs from `core`.
- Features do not import each other's internals.
- Backend modules depend inward on their own domain and outward only on other modules' public application APIs.
- Security is enforced by the backend. Frontend guards and hidden navigation are usability controls only.
- New API routes are deny-by-default and require an explicit permission plus ownership checks for customer-owned resources.
