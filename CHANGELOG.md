# Changelog

Progress notes for the Unikly project. Payment collection and carrier integrations remain out of scope for the current demo checkout.

## 2026-10-20 — Sprint 2 flagship landing page visuals: Bento hero stage, 4-column department showcase, and retail discovery bar

### Storefront and visual foundation

- Expanded `:root` semantic surface, accent, warning, border-radius, elevation, and spatial width tokens (`--shell-width: 1920px`, `--content-width: 1500px`) in `frontend/src/styles.css` (`T2`).
- Upgraded the global storefront header, secondary department bar, and 3-tier retail footer (`app.html`, `app.css`) to span `100%` fluid viewport width (`--shell-width: 1920px` inner command track for header/nav; `--content-width: 1500px` 4-column directory + full-bleed Back-to-top and sub-footer bars) matching Amazon/Walmart spatial proportions while preserving Unikly's forest-and-gold visual identity.
- Built a 3-card flagship Bento hero stage on `home-page` combining the editorial showcase and live catalog metrics with a dynamic Featured Pick card and an Everyday Value ("Finds under $35") spotlight card.
- Added a Walmart/Amazon-inspired 4-column Department Bento grid (`Home & Living`, `Electronics & Audio`, `Kitchen & Dining`, `Outdoor & Carry`) with dual-image collages, live in-stock ratios, starting prices computed from the catalog, and interactive category selection.
- Upgraded the `#shop` catalog discovery toolbar with `In stock only` and `Under $35` quick-filter chips (`aria-pressed`), one-click filter reset, customer-rating sort option, and elevated product cards featuring split dollar/cents retail price lockups, low-stock/out-of-stock pills, and demo/seller provenance badges (`M2`, `M4`, `T1`, `T3`).
- Extended `home-page.spec.ts` to cover department showcase cards, `inStockOnly` and `under35Only` quick filters, and filter reset behavior.

### Verification

- `docker run --rm -v "${PWD}/frontend:/workspace" -w /workspace node:24.21.0-alpine npm run check:architecture` passed (`Angular feature boundaries are valid.`).
- `docker build -t unikly-web-test ./frontend` passed with zero warnings and zero errors.

## 2026-10-20 — Sprint 1 audit fixes: identity hardening, Flyway migration safety, catalog/basket provenance, and accessibility

### Database and configuration

- Backfilled existing accounts as verified (`UPDATE store_users SET email_verified = TRUE`) in `V17__mark_demo_catalog_and_add_identity_email_tokens.sql` before applying the `FALSE` default for new rows so upgrading non-empty databases does not lock out existing users.
- Aligned `docker-compose.yml` and `DevelopmentUserSeederTests` development seed email addresses (`customer@unikly.com`, `admin@unikly.com`, `seller@unikly.com`, `no-reply@unikly.com`) with `application-dev.yml` and `.env.example`.
- Added `.vscode/` to `.gitignore`.

### Backend and security

- Hardened `IdentityEmailFlowService.register` against timing side-channels and unverified-account lockouts/pre-registration hijacking: always hashes the candidate password with Argon2 before checking email existence, and allows re-registration of unverified accounts to update `passwordHash`, `displayName`, and `role` while re-issuing verification.
- Updated `IdentityEmailFlowService` to return `429 Too Many Requests` when registration, verification, or password-recovery rate limits are exceeded instead of silently dropping email dispatch while returning `202 Accepted`.
- Added rate-limiting (`IdentityRequestRateLimiter`) to `POST /api/auth/login` and configured `expiredSessionStrategy` on `maximumSessions(5)` in `AuthSecurityConfiguration` so sessions revoked by password recovery return `401 Unauthorized`.
- Moved `AuthController` authenticated-principal lookups (`requireAuthenticatedUser`) behind `IdentityAccountService` so the API layer no longer injects `StoreUserRepository` directly.
- Added `infrastructure` to `ModuleBoundariesTests.INTERNAL_PACKAGES` and documented `identity`'s internal `infrastructure.email` package in `docs/architecture.md` and `docs/authorization-matrix.md`.

### Frontend and accessibility

- Aligned `CustomerProfile` in `auth.service.ts` with the backend `CustomerProfileService.ProfileView` contract (`id`, `email`, `displayName`, phone, and delivery address fields).
- Made non-empty `/api/products` responses authoritative in `ProductCatalogService` without re-merging deleted `SAMPLE_PRODUCTS`, while retaining sample fallback only when the API fails or returns an empty catalog.
- Added `Demo listing` vs `Seller account #` provenance badges per basket line item on `basket-page` and replaced the unbacked `"30-day hassle-free return guarantee"` with truthful demo checkout guidance.
- Completed the product review rating radio group (`role="radio"`, roving `tabIndex`, and Arrow/Home/End keyboard selection) and dynamic page title updates in `product-detail-page`.
- Fixed `#confirm-password` `aria-invalid`, `aria-describedby`, and inline mismatch feedback on `reset-password`.
- Added route-aware `title` metadata across storefront, catalog, cart, checkout, account, and seller routes, and updated `docs/theme-style-audit.md`.

### Verification

- `docker run --rm -v "${PWD}/frontend:/workspace" -w /workspace node:24.21.0-alpine npm run check:architecture` passed (`Angular feature boundaries are valid.`).
- `docker build -t unikly-web-test ./frontend` passed.
- `docker compose build api` passed.
- Full Java 25 Maven test suite in `maven:3.9.16-eclipse-temurin-25` passed (`Tests run: 29, Failures: 0, Errors: 0, Skipped: 0`).

## 2026-10-20 — Fix storefront production build typing

### Frontend

- Excluded server-owned `emailVerified` from profile update input and used an empty fragment when removing email action tokens from the URL.

### Verification

- `docker build -t unikly-web-test ./frontend` passed.

## 2026-10-20 — Fix API image test compilation

### Build

- Removed the identity authorization test's direct Jackson `ObjectMapper` use; the test compares the full password-recovery response while its existing JSON-path checks verify the message.

### Verification

- `docker compose build api` passed, including test-source compilation.
- The focused identity authorization test was attempted in the Java 25 Maven container, but Maven Central terminated several artifact downloads early.

## 2026-10-20 — Sprint 1 scope: first impression and account access

### Product plan

- Recorded the agreed first-sprint screens: shared header/footer, landing page, registration, login, and account recovery.
- Linked the sprint to M1, M2, M4, M14, and the storefront theme audit. Added acceptance for truthful positioning, demo listing provenance, accessible responsive states, and secure non-enumerating recovery.
- Kept competitor differentiation dependent on a selected customer/category and evidence. Production email delivery remains an explicit M14 gate until a sender and operational owner are selected.

### Verification

- Documentation links, roadmap cross-references, and final diff reviewed; `git diff --check` passed.
- This is a planning increment only. No application code was changed, and no build, automated test, or live browser flow was run. Sprint implementation remains planned.

## 2026-10-20 — Current marketplace SRS, phased roadmap, and storefront style audit

### Product scope and progress

- Added `docs/UNIKLY_SRS_v4.md` as the active marketplace requirements baseline. It distinguishes existing code from production evidence, assigns stable requirement IDs, records open market decisions, and marks payment, carrier, settlement, and scale complexity as gated work.
- Added `docs/product-roadmap.md` with small acceptance-based increments, dependency decisions, status rules, and a shared progress handoff for Codex, Gemini, and other contributors. Final review clarified email verification, operator account lifecycle, scoped support-data access, responsive launch checks, ownership denials, verified-review acceptance, and manual payment-risk ownership before launch; automated risk and catalog identity work remain evidence-gated.
- Repointed project, architecture, skill, and Gemini guidance to the same SRS and roadmap; preserved v2/v3 SRS and the old onboarding guide as historical references.
- Kept launch gates in `docs/release-readiness.md` and completion evidence in this changelog so active work, release proof, and history have separate clear roles.

### Storefront theme

- Added `docs/theme-style-audit.md` with source-backed findings and a staged design sequence covering trust claims, interaction accessibility, shared tokens, responsive targets, media, and route titles.
- No storefront behavior or styles were changed in this documentation increment; rendered browser inspection remains an open gate.

### Verification

- Documentation link targets, requirement/milestone cross-references, status consistency, and final diff were reviewed; no application build or live-stack run was needed for this documentation-only increment.
- M0 was pushed as `b1e001e` on `docs/gemini-ecc-workflow`; the separate production and Compose smoke gates remain open in `docs/release-readiness.md`.

## 2026-10-20 — Enforced module boundaries and delivery tracking

### Architecture and storefront

- Replaced cross-module repository/entity access in catalog, cart, orders, reviews, and profile flows with application-service contracts and immutable identity/product records.
- Moved catalog rating aggregation behind the reviews API; catalog product operations no longer query the reviews repository.
- Split Angular pages into storefront, catalog, cart, and seller features. The app router now composes only route arrays exported through feature `public-api.ts` files, and buyer order routing is owned by the account feature.
- Added ArchUnit checks for cross-module private-package access and module dependency cycles.

### Authorization and release tracking

- Limited public review creation to buyers with eligible verified purchases. Administrators retain review moderation permission and cannot publish storefront reviews.
- Added a public aggregate-rating endpoint for catalog cards and product listings; persisted products now show only ratings returned by real review aggregates.
- Marked seeded demo reviews as unverified because the sample buyer has no matching orders, so fixture data cannot claim a verified purchase without evidence.
- Added GitHub Actions verification for backend tests/architecture checks and the Angular production build, plus work-item and pull-request templates.
- Added `docs/release-readiness.md` to track open launch gates separately from the completed-work changelog.

### Verification

- Passed Java 25 backend main and test source compilation with test execution skipped, the Angular feature-boundary check in Node 24, and the Angular production Docker build.
- The isolated Compose smoke flow remains unverified: the exact pinned Temurin runtime image was downloading too slowly, so the build was stopped before any validation services started.

## 2026-10-20 — Gemini agent orchestration and ECC-aligned project skills

### Agent workflow

- Added four read-only Gemini specialists for Spring, Angular, PostgreSQL/Flyway, and security reviews. The primary session owns implementation, delegates only relevant bounded reviews, and verifies findings.
- Added /deliver and /review Gemini commands for implementation coordination and read-only review. Specialists inherit the selected Gemini model and have file/search tools only; no global or provider-specific settings were added.

### Skill and rule improvements

- Strengthened delivery guidance with conditional specialist delegation and evidence-based synthesis.
- Tightened Spring, Angular, PostgreSQL/Flyway, Git, and project-structure guidance around Unikly's current modular monolith, storefront, data rules, accessibility, and branch safety.
- Kept existing skills because each covers a distinct project workflow; did not delete or replace them with generic ECC material.
- Added docs/ai-agent-workflow.md to map the relevant ECC skills to Unikly skills and explain the Gemini delegation model.

### Verification

- Documentation and agent configuration only. No application build, unit test, or live-stack smoke test was run.
- Gemini CLI is not available in this environment, so native agent discovery could not be exercised here.

## 2026-10-20 — Agent rules, ECC-inspired delivery, and Gemini CLI support

### Agent guidance and skills

- Replaced outdated product and architecture assumptions in AGENTS.md with the active storefront scope, current toolchain versions, safe branch practices, and change-sized verification guidance.
- Added the feature-delivery skill for focused planning, implementation, reviewer-style diff inspection, and evidence-based verification.
- Updated the project structure, Spring, security, Docker, Git, and smoke-testing skills to match the current code. Removed the machine-specific smoke-test path and assumed development credentials.

### Gemini and project documentation

- Added GEMINI.md as a Gemini CLI context entry point importing AGENTS.md. Gemini CLI discovers the shared skills in .agents/skills/.
- Documented Gemini CLI setup and the pinned Docker development commands in README.md.
- Updated architecture and authorization documentation to match current modules and security matchers. Marked the old onboarding guide as historical.

### Verification

- Documentation and agent configuration changes only. No application build, unit test, or live-stack smoke test was run.

## 2026-10-02 — Git workflow skill, star rating color fix, and verified purchase review gate

### Developer Tooling & Git Workflow
- Created AI Agent & Developer Skill [`.agents/skills/git-workflow/SKILL.md`](.agents/skills/git-workflow/SKILL.md):
  - Codified branch prefix conventions (`feat/`, `fix/`, `refactor/`, `perf/`, `docs/`, `chore/`).
  - Standardized branch lifecycle, conventional commits, toolchain-isolated verification, and clean merge procedures.
  - Added Rule 4 to [`AGENTS.md`](AGENTS.md) requiring topic branching for all future increments and fixes.

### Frontend Star Rating Display Fix
- Resolved an issue where Lucide star icons appeared grey even on reviewed products:
  - Switched from `[class.filled]="..."` to dynamic `[class]="... ? '... filled' : '...'"` bindings across [`HomePage`](frontend/src/app/features/storefront/pages/home-page/home-page.html) and [`ProductDetailPage`](frontend/src/app/features/catalog/pages/product-detail-page/product-detail-page.html) to prevent Lucide's `[attr.class]` binding from wiping out the active class.
  - Added `:host ::ng-deep` encapsulated CSS selectors in [`home-page.css`](frontend/src/app/features/storefront/pages/home-page/home-page.css) and [`product-detail-page.css`](frontend/src/app/features/catalog/pages/product-detail-page/product-detail-page.css) targeting `<svg:path>` elements directly to guarantee vibrant golden fill (`#f59e0b`) and amber outline (`#d97706`).

### Authenticity & Verified Purchase Review Gate
- Strengthened review integrity in [`ProductReviewService`](backend/src/main/java/com/unikly/store/reviews/application/ProductReviewService.java):
  - Enforced that only buyers who have placed and confirmed an order for a product can submit a review (`HTTP 403 Forbidden` if unpurchased).
  - Updated `canReview` calculation in review summary responses to require verified purchase status.
- Enhanced UX on [`ProductDetailPage`](frontend/src/app/features/catalog/pages/product-detail-page/product-detail-page.html):
  - Verified buyers see "Write a verified review" with a confirmed purchase status hint.
  - Authenticated buyers without a purchase see a clear "Verified Purchase Required" notice explaining the policy, with a one-click CTA to add the product to their basket or view cart.
- Added comprehensive integration tests in [`ProductReviewTests`](backend/src/test/java/com/unikly/store/reviews/ProductReviewTests.java); verified purchase gating with 25 passing backend tests.
- Updated smoke test suite verifying that unpurchased review attempts are blocked with 403 Forbidden while verified reviews publish successfully.

### Frontend Architecture & Icon Standard
- Standardized the entire Angular web client on [`@lucide/angular`](https://lucide.dev) standalone components, eliminating unicode emojis and verbose raw SVG boilerplate.
- Re-architected icons across all root and feature components with clean tree-shaken imports:
  - [`App`](frontend/src/app/app.ts): Replaced search, account, and header cart icons with `LucideSearch`, `LucideUser`, and `LucideShoppingCart`.
  - [`HomePage`](frontend/src/app/features/storefront/pages/home-page): Replaced catalog card rating stars and stepper controls with `LucideStar`, `LucideTrash`, `LucideMinus`, and `LucidePlus`.
  - [`BasketPage`](frontend/src/app/features/cart/pages/basket-page): Replaced delivery threshold icons, basket item stepper buttons, trust guarantee checkmarks, and empty state cart graphics with `LucideCheck`, `LucideTruck`, `LucideTrash`, `LucideMinus`, `LucidePlus`, and `LucideShoppingCart`.
  - [`ProductDetailPage`](frontend/src/app/features/catalog/pages/product-detail-page): Replaced rating header stars, rating breakdown charts, interactive star rating picker, verified buyer shield badges, review author avatars, empty feedback indicators, and action buttons with `LucideStar`, `LucideSquarePen`, `LucideShieldCheck`, `LucideCheck`, `LucideCircleAlert`, `LucideMessageSquare`, `LucideUser`, and `LucideX`.
- Updated CSS styling across [`home-page.css`](frontend/src/app/features/storefront/pages/home-page/home-page.css) and [`product-detail-page.css`](frontend/src/app/features/catalog/pages/product-detail-page/product-detail-page.css) ensuring SVG path elements inherit fill and stroke values dynamically for rating states.
- Formalized Rule 6 in [`AGENTS.md`](AGENTS.md) mandating `@lucide/angular` standalone components with SVG attribute selectors (`<svg lucideName>`) for all icons across the repository.

### Verification
- Verified Angular client compiles cleanly with 0 errors and 0 warnings via isolated Docker build (`docker build -t unikly-web-test ./frontend`).
- Verified all 13 end-to-end smoke tests pass with 100% success against live Docker Compose topology.

### Database Schema & Migrations
- Added Flyway migration [`V16__create_product_reviews.sql`](backend/src/main/resources/db/migration/V16__create_product_reviews.sql):
  - Created `product_reviews` table with foreign keys `product_id REFERENCES catalog_products(id) ON DELETE CASCADE` and `buyer_id REFERENCES store_users(id) ON DELETE CASCADE`.
  - Added constraints: rating check (`rating >= 1 AND rating <= 5`), unique buyer constraint `uq_product_reviews_product_buyer (product_id, buyer_id)` for single review per buyer/product, and author name/text fields.
  - Added relational performance indexes `ix_product_reviews_product_id`, `ix_product_reviews_buyer_id`, and `ix_product_reviews_product_created`.

### Domain Model, Persistence & Security
- Created [`ProductReview`](backend/src/main/java/com/unikly/store/reviews/domain/ProductReview.java) domain entity with rating domain validation and review update logic.
- Created [`ProductReviewRepository`](backend/src/main/java/com/unikly/store/reviews/persistence/ProductReviewRepository.java) providing review queries, buyer existence checks, and aggregate rating grouping (`findAggregateRatings`).
- Added `hasPurchasedProduct` query to [`CustomerOrderRepository`](backend/src/main/java/com/unikly/store/orders/persistence/CustomerOrderRepository.java) to authoritatively verify buyer purchase status against placed orders.
- Updated [`StorePermission`](backend/src/main/java/com/unikly/store/identity/domain/StorePermission.java) and [`StoreRole`](backend/src/main/java/com/unikly/store/identity/domain/StoreRole.java) with `REVIEW_CREATE_SELF`.
- Configured [`AuthSecurityConfiguration`](backend/src/main/java/com/unikly/store/platform/security/AuthSecurityConfiguration.java):
  - Public `GET /api/products/*/reviews`
  - Authenticated buyer `POST /api/products/*/reviews` and `DELETE /api/products/*/reviews/mine` with `REVIEW_CREATE_SELF`
  - Admin moderation `DELETE /api/reviews/*` with `REVIEW_MODERATE`

### Application Layer & Controllers
- Created [`ReviewRequests`](backend/src/main/java/com/unikly/store/reviews/application/ReviewRequests.java) and [`ReviewViews`](backend/src/main/java/com/unikly/store/reviews/application/ReviewViews.java) immutable record DTOs.
- Created [`ProductReviewService`](backend/src/main/java/com/unikly/store/reviews/application/ProductReviewService.java) providing review summaries with star breakdowns (1-5 stars), verified purchase detection, upsert semantics, and moderation.
- Updated [`CatalogProductService`](backend/src/main/java/com/unikly/store/catalog/application/CatalogProductService.java) to dynamically aggregate real star ratings and review counts from `product_reviews`.
- Seeded realistic verified reviews in [`DevelopmentUserSeeder`](backend/src/main/java/com/unikly/store/platform/bootstrap/DevelopmentUserSeeder.java).
- Created REST controllers [`ProductReviewController`](backend/src/main/java/com/unikly/store/reviews/api/ProductReviewController.java) and [`AdminReviewModerationController`](backend/src/main/java/com/unikly/store/reviews/api/AdminReviewModerationController.java).

### Frontend UI & Vector Icon System
- Codified strict vector icon rule in [`AGENTS.md`](AGENTS.md): all unicode emojis replaced with accessible inline SVG vector icons across the web client.
- Replaced emojis and text steppers across [`BasketPage`](frontend/src/app/features/cart/pages/basket-page), [`HomePage`](frontend/src/app/features/storefront/pages/home-page), and [`ProductDetailPage`](frontend/src/app/features/catalog/pages/product-detail-page) with crisp SVG vector icons.
- Created [`ReviewsService`](frontend/src/app/core/reviews/reviews.service.ts) Angular core HTTP service.
- Completely enhanced [`ProductDetailPage`](frontend/src/app/features/catalog/pages/product-detail-page):
  - Authoritative average rating header with smooth anchor link scroll to reviews section.
  - Overall score banner, star breakdown bars (5★ down to 1★) with counts and percentages.
  - "Verified Purchase" badges for confirmed buyers.
  - Interactive review submission and editing form with interactive SVG star rating selector.
  - Buyer review management (edit/delete own review) and prompt for unauthenticated shoppers.

### Verification
- Added 5 comprehensive integration tests in [`ProductReviewTests`](backend/src/test/java/com/unikly/store/reviews/ProductReviewTests.java); all 24 backend tests passing in Docker toolchain.
- Verified Angular frontend compiles cleanly in Docker with 0 errors and 0 warnings (`docker build -t unikly-web-test ./frontend`).
- Verified 13/13 live end-to-end smoke tests against running Docker Compose topology, validating CSRF, cart persistence, verified purchase detection, and review publishing.

## 2026-10-02 — Interactive cart controls and ecommerce value-add features

### Frontend UX & Reactive State Management
- Enhanced [`CartService`](frontend/src/app/core/cart/cart.service.ts) with `updateQuantity(productId, quantity, availableQuantity)` and real-time synchronization with `PUT /api/cart/items/{productId}`.
- Re-architected [`BasketPage`](frontend/src/app/features/cart/pages/basket-page):
  - Added accessible `[-] [quantity] [+]` stepper controls with instant stock ceiling enforcement and single-item removal.
  - Added product thumbnail previews with lazy loading and fallback placeholder handling.
  - Added unit price breakdown, dynamic line totals, and stock status badges (`In stock`, `Only X left in stock`, `Max in basket`).
  - Added dynamic Free Delivery progress bar (`Add $X.XX more for FREE Standard Delivery` vs. `You've unlocked FREE Standard Delivery!`).
  - Redesigned order summary card with item count, delivery estimates, trust guarantees, and sticky desktop positioning.
- Enhanced Catalog & Product Detail UX:
  - Added inline in-basket steppers directly to product grid cards on [`HomePage`](frontend/src/app/features/storefront/pages/home-page) allowing buyers to increment or decrement quantities without leaving the catalog.
  - Added in-basket stepper and direct "View basket" link to [`ProductDetailPage`](frontend/src/app/features/catalog/pages/product-detail-page).
  - Added low-stock alert badges (`Only X left!`) across catalog listings and detail views.

## 2026-10-02 — Server-side cart persistence and guest-to-buyer sync (`CART_MANAGE_SELF`)

### Database Schema & Migrations
- Added Flyway migration [`V15__create_customer_carts.sql`](backend/src/main/resources/db/migration/V15__create_customer_carts.sql):
  - Created `customer_carts` table keyed by UUID with foreign key `buyer_id UNIQUE REFERENCES store_users(id) ON DELETE CASCADE`.
  - Created `customer_cart_items` table with foreign key `cart_id REFERENCES customer_carts(id) ON DELETE CASCADE`, `product_id`, positive quantity constraint (`quantity > 0`), and unique constraint `uq_cart_product (cart_id, product_id)`.
  - Added relational indexes `ix_customer_carts_buyer_id`, `ix_customer_cart_items_cart_id`, and `ix_customer_cart_items_product_id`.

### Domain Entities & Persistence
- Created [`CustomerCart`](backend/src/main/java/com/unikly/store/cart/domain/CustomerCart.java) and [`CustomerCartItem`](backend/src/main/java/com/unikly/store/cart/domain/CustomerCartItem.java) encapsulating domain invariants, item lookups, and quantity updates.
- Created [`CustomerCartRepository`](backend/src/main/java/com/unikly/store/cart/persistence/CustomerCartRepository.java) using `@EntityGraph(attributePaths = "items")` to fetch cart items eagerly without N+1 query overhead.

### Application Services & Authoritative Calculations
- Defined immutable request records in [`CartRequests`](backend/src/main/java/com/unikly/store/cart/application/CartRequests.java) (`AddItem`, `UpdateItem`, `MergeItem`, `MergeRequest`) with Jakarta `@Valid` constraints.
- Defined response view records in [`CartViews`](backend/src/main/java/com/unikly/store/cart/application/CartViews.java) (`CartView`, `CartItemView`) with batch product resolution (`products.findAllById`) and exact `BigDecimal` line totals.
- Implemented [`CustomerCartService`](backend/src/main/java/com/unikly/store/cart/application/CustomerCartService.java):
  - Real-time stock validation: rejects additions when $Q_{cart} + Q_{add} > \text{stockQuantity}$ with `409 CONFLICT`.
  - Graceful guest cart merge: caps merged item quantities to available inventory without breaking buyer authentication.
  - Cart clearing: provides `clearCart` and `clearCartByBuyerId`.

### Order Placement & Security Configuration
- Configured [`AuthSecurityConfiguration`](backend/src/main/java/com/unikly/store/platform/security/AuthSecurityConfiguration.java) to guard `/api/cart/**` endpoints with `StorePermission.CART_MANAGE_SELF.authority()`.
- Exposed REST controller [`CustomerCartController`](backend/src/main/java/com/unikly/store/cart/api/CustomerCartController.java) (`GET`, `POST`, `PUT`, `DELETE` operations).
- Connected cart lifecycle to [`CustomerOrderService.create`](backend/src/main/java/com/unikly/store/orders/application/CustomerOrderService.java), automatically clearing the buyer's persistent database cart upon successful order placement.

### Frontend Reactive Synchronization
- Modernized Angular [`CartService`](frontend/src/app/core/cart/cart.service.ts) using constructor-less `inject()` and Angular Signals.
- Implemented reactive `effect()`: automatically synchronizes buyer carts with `/api/cart`, merges any guest `localStorage` items upon sign-in, and maintains local storage fallback for unauthenticated visitors.

### Automated & Live Verification
- Added 5 comprehensive integration tests in [`CustomerCartTests`](backend/src/test/java/com/unikly/store/cart/CustomerCartTests.java) (19/19 backend tests passing in isolated Docker container).
- Updated [`cart.service.spec.ts`](frontend/src/app/core/cart/cart.service.spec.ts) with `provideHttpClientTesting()`.
- Verified production build with `docker build -t unikly-web-test ./frontend`.
- Executed 12-step live smoke test suite against running Docker Compose stack, verifying full end-to-end lifecycle including CSRF, cart persistence, merge, order placement, and database auto-clearing.

## 2026-10-02 — Architecture and best practice audit refinements

- Completed end-to-end architecture and implementation review against `AGENTS.md` and industry best practice skills.
- Optimized backend seller order queries with `@Transactional(readOnly = true)` on `CustomerOrderService.listForSeller`.
- Added database index `ix_customer_order_items_seller_id` via migration `V14` to accelerate seller order item joins.
- Modernized Angular `ProductDetailPage` to 100% constructor-less `inject()` and reactive `toSignal()` route parameter resolution.
- Added `takeUntilDestroyed()` cleanup to `HomePage` search query parameter subscription.

## 2026-10-01 — Delivery options and profile address auto-fill

- Auto-fill buyer delivery contact and address details from saved customer profile upon entering checkout.
- Added delivery method options (Standard Delivery with free shipping over $50 vs. Express Delivery for $15.00).
- Authoritatively calculate delivery fees and order totals on the server.
- Persist delivery method and fee with the order; expose delivery method in buyer and seller order management views.
- Seed catalog products for the dev seller account (`seller@unikly.local`) and reassign sample items to prevent order item seller foreign key constraint violations.
- Scoped `AuthExceptionHandler` to `AuthController` to avoid obscuring unrelated database constraint errors.
- Fixed 404 Unsplash image URL for the Anodized Aluminum Mechanical Pencil Set product via migration V13 and `DevelopmentUserSeeder`.
- Added image load error fallback handlers across product grid, detail view, and seller cards to gracefully fall back to `/product-placeholder.svg`.

## 2026-10-01 — Buyer order cancellation

- Let buyers cancel only their own orders while every item is still waiting to be processed.
- Restore the canceled items' inventory and expose cancellation in buyer order history.
- Mark the order and its items as canceled, and protect the endpoint with `ORDER_CANCEL_SELF` plus buyer ownership checks.

## 2026-09-30 — Allow buyer delivery confirmation

- Authorize the order delivery confirmation route for buyers with order read access.
- Keep the endpoint protected by buyer ownership checks in the order service.

## 2026-09-30 — Buyer delivery confirmation

- Let buyers confirm delivery after every item in their order has shipped.
- Update all shipped items to delivered so the buyer and each seller see the completed order state.
- Restrict confirmation to the buyer who placed the order and reject premature confirmations.

## 2026-09-30 — Seller shipment tracking details

- Ask sellers for a carrier and tracking URL when they mark an order as shipped.
- Validate tracking URLs as absolute HTTP or HTTPS links and save details with the seller's order items.
- Show buyers a Track package link for shipped items in order history.

## 2026-09-30 — Honor API port in Angular dev proxy

- Read the API port from the ignored root `.env` file so Angular dev requests follow the Compose port mapping.
- Avoid sending authenticated API requests to a stale default port when `API_HOST_PORT` is customized.

## 2026-09-30 — Buyer item fulfillment visibility

- Include each item's seller fulfillment status in buyer order history.
- Clarify the seller's progress separately for items in mixed-seller orders.

## 2026-09-29 — Seller order fulfillment

- Add a seller-only order inbox that includes delivery details and only the seller's own order lines.
- Let sellers advance their items through placed, processing, shipped, and delivered states.
- Show buyers the order's least-complete fulfillment state across all sellers.
- Enforce seller ownership and one-step status transitions on the server.

## 2026-09-29 — Remove committed development passwords

- Require local environment configuration for the database password and optional seeded development accounts.
- Disable development account seeding by default and document `.env` setup without publishing passwords.
- Generate temporary passwords at test runtime instead of storing reusable test credentials in the repository.

## 2026-09-29 — Expired checkout session recovery

- Detect an expired buyer session during checkout, keep the basket, and prompt the buyer to sign in again.
- Clarify the cause: development API restarts invalidate in-memory login sessions.

## 2026-09-29 — Persist featured catalog products

- Seed the storefront sample products into the shared database catalog so checkout can reserve their inventory.
- Merge API records with the featured presentation data by product ID to prevent duplicate cards and keep server stock authoritative.
- Return a not-found error for stale basket product IDs instead of reporting a stock conflict.

## 2026-09-29 — Persistent orders and purchase history

- Save orders and delivery details to the signed-in buyer account, retaining a stable order reference and purchased-item snapshots.
- Decrease product inventory atomically when an order is accepted; reject an order if stock has changed.
- Add a buyer order history view with placed date, items, quantity, total, and order status.
- Explain that shipment tracking and payment are not integrated yet.

## 2026-09-29 — Demo checkout

- Added a delivery details form reachable from the basket.
- Added an order summary, client-side stock availability validation, and a demo confirmation reference.
- Clear the basket after a successful demo order.
- Clearly identify checkout as a demo without payment or delivery processing. (Order persistence and saved delivery details were added in the following increment.)

## 2026-09-29 — Product stock and availability

- Added seller-managed stock quantities to product creation and editing.
- Added stock availability indicators to product listings and details.
- Prevented basket quantities from exceeding the available stock.
- Added the database migration that introduced stock quantities for existing catalog listings.
