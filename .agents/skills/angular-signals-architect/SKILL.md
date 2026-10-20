---
name: angular-signals-architect
description: >-
  Build and review Unikly's Angular 22.2 standalone storefront with Signals, accessible responsive UI, and the established feature boundaries. Use for Angular components, templates, state, forms, routes, and styling.
---

# Angular Signals architect

Before changing a screen or state flow, inspect the feature's existing route, component, service, tests, and styles. Follow the installed Angular version and repository patterns rather than importing React or older Angular conventions.

## Architecture and reactivity

- Keep components, pipes, and directives standalone. Do not introduce NgModules.
- Use inject() for dependency injection and Angular control flow (@if, @for with a stable track expression, @switch) instead of legacy structural directives.
- Use signal() for mutable view state and computed() for derived state. Reserve effect() for synchronization with an external system; do not copy derived state between signals.
- Keep shared API and state services in frontend/src/app/core/. Keep page and feature-only code in the owning frontend/src/app/features/ area. Preserve lazy routes and feature public entry points.
- Compose routes in app.routes.ts only from route arrays exported through each feature's public-api.ts. Feature route files may import their own page internals; another feature and the app shell must not import those internals directly.
- Keep storefront, catalog, basket/checkout, and seller workflow pages in their owning features. Put the shared catalog client and product state in core/catalog/; do not make one landing-page feature a catch-all for unrelated flows.
- Prefer the established service and form patterns. Use RxJS where HTTP or asynchronous stream composition benefits from it; do not add manual subscriptions where a signal interop or template binding is clearer.
- Keep components focused on view state and user interaction. Do not put server business rules or backend data ownership decisions in the browser.

## Storefront behavior

- The server owns authorization, stock checks, product prices, delivery fees, and order totals. Display values returned by the API and never treat client-calculated totals or hidden controls as authoritative.
- Preserve session-cookie and CSRF behavior. Refresh the CSRF token after login rotates the session.
- Handle loading, success, empty, and failure states for asynchronous screens. Prevent duplicate submissions and give errors a clear recovery path.
- Use the existing CSS variables and component styling conventions. Keep layouts usable at narrow widths and preserve visible focus and keyboard operation.
- Use the existing Lucide Angular icons with individual standalone imports; do not use emoji icons or add verbose inline SVGs.

## Accessibility and assets

- Use semantic HTML and accessible names for controls. Keep keyboard order logical, provide visible focus, and announce relevant dynamic results with status or live-region semantics.
- Give images useful alt text. Follow the existing local-asset and external-image fallback patterns; do not add a global error handler to assets that do not use the network.
- Use lazy loading for suitable below-the-fold product imagery. Avoid layout shifts by preserving image dimensions or aspect ratio.

## Verification

For behavior changes, cover the affected interaction and states with focused frontend tests. Build using the Node 24 Docker workflow in ../docker-toolchain-isolated/SKILL.md. For customer-flow, API, auth, or persistence changes, coordinate with the backend and use ../e2e-smoke-testing/SKILL.md as applicable.
