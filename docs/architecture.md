# Application architecture

## Product boundary

Unikly is an e-commerce storefront. The current application supports public catalog browsing, buyer and seller accounts, a cart, checkout and orders, profile management, seller fulfillment, and product reviews. Payment collection and carrier integrations are outside the current implementation.

The current product requirements and capability status are in [UNIKLY_SRS_v4.md](UNIKLY_SRS_v4.md), sequenced in the [implementation roadmap](product-roadmap.md). The v3/v2 SRS files and onboarding guide are historical snapshots, including the superseded freelance-services marketplace direction.

## Backend: modular monolith

The backend is one Spring Boot application. Business capabilities are modules under backend/src/main/java/com/unikly/store/:

- identity: registration, session authentication, account permissions, and security integration.
- catalog: product listing, seller-owned inventory, and catalog API.
- cart: persistent customer cart and cart API.
- orders: order creation, buyer order history, cancellation, delivery confirmation, and seller fulfillment.
- profile: the authenticated customer's contact and delivery profile.
- reviews: product reviews, verified-purchase checks, and moderation endpoints.
- platform: cross-cutting HTTP and security configuration.

Modules use the API, application, domain, and persistence layers where needed. identity also contains its Spring Security user-details adapter and outbound email delivery adapter (`infrastructure`). Keep infrastructure configuration in platform, not in business domains.

A module owns its entities, repositories, and use cases. Cross-module operations use the owning module's public application service and exchange immutable application records; business modules must not couple through another module's repositories or internal entities. `platform` is the technical composition module: `platform.security` may reference identity security primitives, while `platform.bootstrap` may access module persistence only for dev-profile fixture seeding. Neither exception may implement customer business flows. Add only packages that have working code. Do not create future modules in advance.

`ModuleBoundariesTests` verifies that business modules do not import another module's `api/`, `domain/`, `persistence/`, `security/`, or `infrastructure/` packages and that application modules remain acyclic.

Flyway migrations are in backend/src/main/resources/db/migration/. Backend tests mirror the business modules under backend/src/test/java/com/unikly/store/.

## Frontend: standalone Angular features

The Angular app is composed from a small application shell and lazy feature routes.

- frontend/src/app/core/ contains shared services for catalog, cart, identity, orders, and reviews.
- frontend/src/app/features/identity/ owns registration, sign-in, and password recovery screens.
- frontend/src/app/features/account/ owns account settings and buyer order history.
- frontend/src/app/features/storefront/ owns the storefront landing page.
- frontend/src/app/features/catalog/ owns product detail and catalog presentation.
- frontend/src/app/features/cart/ owns the basket and checkout pages.
- frontend/src/app/features/seller/ owns seller product and fulfillment workflows.
- frontend/src/app/app.routes.ts composes feature route entry points. It imports only route arrays exported from each feature's `public-api.ts`; feature internals stay behind those entry points.
- Shared catalog API/state lives in `frontend/src/app/core/catalog/` so catalog, seller, cart, and checkout screens use one server-backed product model.

`npm run check:architecture` verifies that the application shell and other features do not import feature internals directly. CI runs this check before the Angular production build.

Keep page components and feature-only services inside their feature. Put cross-feature API clients and state in core. Do not import another feature's internal pages or components.

## Security boundary

The backend is authoritative for authentication, permissions, resource ownership, stock and order calculations. Product review summaries are served by the reviews module so the catalog module does not query the reviews repository. Frontend guards and hidden controls improve navigation but do not grant access. New API routes must be explicitly authorized; unmatched /api/** routes are denied.

See [docs/authorization-matrix.md](authorization-matrix.md) for the current route and permission mapping.
