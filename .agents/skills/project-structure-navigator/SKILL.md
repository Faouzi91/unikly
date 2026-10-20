---
name: project-structure-navigator
description: >-
  Current Unikly architecture and file placement guide. Use when locating code, choosing a module boundary, or checking which product documents are current.
---

# Unikly project structure

Use the active product and architecture docs as sources of truth: [README.md](../../../README.md), [UNIKLY_SRS_v4.md](../../../docs/UNIKLY_SRS_v4.md), [implementation roadmap](../../../docs/product-roadmap.md), and [docs/architecture.md](../../../docs/architecture.md). SRS v2/v3 and the onboarding guide are historical snapshots.

## Current source layout

Backend modules under backend/src/main/java/com/unikly/store/:

- identity/: authentication, account permissions, and security adapter.
- catalog/: public product browsing and seller product management.
- cart/: persistent cart and cart operations.
- orders/: checkout, order history, cancellation, delivery confirmation, and seller fulfillment.
- profile/: authenticated customer profile and delivery details.
- reviews/: product reviews and moderation.
- platform/: cross-cutting HTTP and security configuration.

Business modules use api/, application/, domain/, and persistence/ as needed. identity/ also has security/. Do not create empty layers or future modules.

Frontend under frontend/src/app/:

- core/identity, core/cart, core/orders, and core/reviews: shared API services and state.
- features/identity: registration, sign-in, and password recovery.
- features/account: account settings and buyer order history.
- features/storefront: storefront landing page.
- features/catalog: product detail and catalog presentation.
- features/cart: basket and checkout.
- features/seller: seller products and fulfillment.
- core/catalog: shared catalog API, product state, and demo product fixtures.
- app.routes.ts: app-level composition of route arrays exported through feature public APIs.

## File placement

| Work | Put it here |
| --- | --- |
| New migration | backend/src/main/resources/db/migration/V-number__description.sql |
| Entity, value object, or enum | Owning backend module's domain/ |
| Repository or persistence adapter | Owning module's persistence/ |
| Use case or transaction boundary | Owning module's application/ |
| Controller and API error mapping | Owning module's api/ |
| Shared Angular API/state | frontend/src/app/core/<area>/ |
| Feature page, route, or feature-only data service | frontend/src/app/features/<feature>/ |
| App-wide composition or provider | frontend/src/app/ |
| Static browser asset | frontend/public/ |

## Boundaries to keep

- Keep a module's model and repository private to that module. Call across modules through public application APIs.
- Keep UI pages out of core/. Keep feature-only code out of the application shell.
- Keep Angular features behind public-api.ts; do not import another feature's internal page/component.
- Keep app.routes.ts limited to feature public APIs. A feature may import its own internals from its route configuration.
- Add a forward Flyway migration for schema changes; never modify an applied migration.
- For endpoint permissions and ownership rules, read [docs/authorization-matrix.md](../../../docs/authorization-matrix.md).

## Agent configuration

- Shared project skills are under .agents/skills/.
- Gemini local specialist reviewers are under .gemini/agents/; reusable Gemini commands are under .gemini/commands/.
- The primary session coordinates reviewers. Specialist agents are read-only and are not implementation agents.
