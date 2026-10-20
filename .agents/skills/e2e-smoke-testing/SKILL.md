---
name: e2e-smoke-testing
description: >-
  Verify Unikly customer flows against the local Docker Compose stack. Use for changes to authentication, catalog, cart, checkout, orders, fulfillment, reviews, persistence, or release readiness.
---

# End-to-end smoke testing

Use this skill when a code change can alter a customer-visible flow, server authorization, persistence, or service wiring. A documentation-only change does not need an application smoke test.

## Safe test setup

- Target the local development Compose stack only, never a production or shared environment.
- Check docker compose ps and service health first. The default local web and API ports are 4200 and 8080; .env can override the API and database host ports.
- Use disposable local accounts and data. Do not assume seeded users exist or reuse passwords from documentation. Development seeding is opt-in and configured through ignored .env.
- Do not print .env, credentials, session cookies, CSRF values, or personal data in reports.
- Do not clear the database volume to prepare a smoke test.

## Choose the affected flow

Use the current README, SRS v3, controllers, and tests to confirm route names and business rules. Cover the narrowest complete flow changed, including the relevant frontend, API, authorization, and persistence boundaries.

Common flows include:

- Public catalog browsing, search, and product detail/review reads.
- Buyer registration or sign-in, profile updates, cart changes, checkout, and order history.
- Seller product management and fulfillment for products owned by that seller.
- Buyer cancellation or delivery confirmation under the current order state rules.
- Review submission after an eligible verified purchase, own-review deletion, and authorized moderation.

Do not reuse historical delivery prices, endpoint assumptions, or seeded credentials from old smoke-test instructions.

## Session and CSRF behavior

Unikly uses server-side sessions. Preserve the session cookie between requests. Initialize CSRF with GET /api/auth/csrf; after login, fetch it again because the session ID rotates. Send the current token in X-XSRF-TOKEN with state-changing requests such as POST, PUT, PATCH, and DELETE.

## Evidence and completion

- Prefer a repository-maintained smoke script if one exists and matches the current application. Otherwise, exercise the flow manually in the local browser or with HTTP requests.
- Check the user-visible result and the expected API/database outcome without exposing personal or secret data.
- Record which flow, environment, and checks passed. If the stack or a dependency is unavailable, state that the live check remains unverified.
