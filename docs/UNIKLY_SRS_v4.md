# Unikly Marketplace — Software Requirements Specification

**Version:** 4.0
**Updated:** 2026-10-20
**Status:** Active product baseline; launch-market decisions remain open
**Supersedes:** `UNIKLY_SRS_v3.docx` for current product scope. The v3 document is retained as a historical snapshot.

## 1. Purpose and product direction

Unikly is a multi-seller e-commerce marketplace. The product should earn buyer trust and seller confidence in a clearly chosen first market, then expand based on customer demand, operational capacity, and measured system limits. Amazon and Walmart are useful category benchmarks for marketplace capabilities; matching their size, catalog breadth, logistics network, or infrastructure is not a near-term requirement.

The product owner must decide the initial customer segment, launch country/region, first product category or seller wedge, supported currency, fulfillment promise, and the reason buyers and sellers should choose Unikly. These decisions are open inputs to market-specific payment, tax, returns, delivery, privacy, and seller-payout requirements. Until those decisions are recorded, do not assume a country, payment provider, currency, legal regime, or delivery service.

### Product principles

1. Build a dependable browse-to-order and seller-fulfillment path before adding advanced commerce or scale infrastructure.
2. Make prices, stock, order state, permissions, ownership, and policy claims authoritative on the server.
3. Keep the Spring Boot backend a modular monolith and the Angular client feature-oriented while those structures serve the team and measured workload.
4. Release narrow end-to-end increments with observable acceptance criteria and recorded verification evidence.
5. Treat trust, support, privacy, accessibility, security, and operating procedures as product capabilities, not late polish.
6. Defer expensive or operationally complex capabilities until an explicit market need or measured bottleneck justifies them.

## 2. Stakeholders and roles

- **Buyer:** discovers products, manages a basket/profile, places and follows orders, and may review eligible purchases.
- **Seller:** manages their own listings and stock, receives their own order lines, and advances fulfillment state.
- **Administrator/support operator:** handles controlled moderation and customer/seller issues through separately permissioned tools.
- **Product and operations owners:** decide launch-market policies, support processes, release gates, and acceptable service targets.
- **Engineering team and coding agents:** deliver and verify increments under `AGENTS.md`, the architecture guide, and the shared roadmap.

The server-side role and permission contract is maintained in [the authorization matrix](authorization-matrix.md). Roles must grant only the operations each user needs; hiding a client control is never an authorization boundary.

## 3. Current product baseline

This is a code-presence assessment, not a production-readiness declaration. “Implemented” means the capability exists in the current source; applicable end-to-end, operational, and release evidence is still governed by [the roadmap](product-roadmap.md) and [release gates](release-readiness.md).

| Capability | Current status | Notes / remaining gap |
| --- | --- | --- |
| Buyer and seller accounts | Partial | Session authentication and server permissions exist. Seller identity/business verification, email verification, and email-based password recovery are not complete. |
| Buyer profile | Partial | Contact and default delivery details exist. Retention, export, and deletion processes need definition for the launch market. |
| Seller store profile | Open | No complete seller storefront profile with public business details and seller support contact. |
| Catalog and inventory | Partial | Persistent products, seller-owned CRUD, stock validation, and stock reservation exist. Managed categories, listing lifecycle, moderation, category-specific attributes/variants, canonical product/offer identity, and durable media management remain gaps. |
| Browse and search | Partial | The storefront supports client-side filtering/sorting of loaded products. Search across name and description on the server, scalable result paging, and managed category data remain open. |
| Basket and checkout | Partial | Persistent signed-in cart, guest cart support, order creation, server-side totals, and delivery options exist. Payment collection is absent; currency is not modeled end-to-end; duplicate-safe order submission needs an explicit contract. |
| Orders and fulfillment | Partial | Buyers can view/cancel eligible orders and confirm delivery; sellers update fulfillment for their own items. Carrier integration, split seller shipments, return/refund cases, and operator intervention are absent. |
| Reviews | Partial | Review submission, summaries, and moderation exist. Eligibility must be tied to the agreed completed-purchase state and the policy should be made explicit. |
| Administration and support | Partial | Permissioned review moderation and an access-check example exist. Seller/listing/account administration and customer order issue workflows remain open. |
| Notifications and reporting | Open | Transactional lifecycle notifications, useful seller reports, and operator reporting are not complete. |
| Production operations | Open | Restore rehearsal, monitoring/alerts, production TLS/secrets, incident procedures, and measured launch-load evidence remain release gates. |

Seeded/sample content must be visibly identified as demo data or tied to a clearly identified seller before public launch. Do not present a fixture as an independently verified real offer.

## 4. Product scope

### 4.1 Near-term scope

- A public catalog and product detail experience with truthful product, stock, seller, price, delivery, and review information.
- Buyer registration, sign-in/out, profile, basket, checkout/order placement, order history, and order status.
- Seller registration, seller-owned product and stock management, seller order visibility, and fulfillment updates.
- Server-enforced authentication, role permissions, resource ownership, validation, stock rules, and monetary calculations.
- Basic administrator/support capabilities needed to keep listings and orders safe for the first market.
- Usable responsive web experience with accessible keyboard interaction, clear errors, empty states, and loading states.

### 4.2 Later, gated by market and evidence

- Payment provider, refunds, platform commission, seller settlement, tax behavior, and payment reconciliation.
- Return/exchange workflows, carrier labels/tracking, delivery quotes, or outsourced fulfillment.
- Seller verification depth, payout onboarding, category-specific listing attributes, and bulk catalog feeds.
- Promotions, deeper search/facets, recommendation or personalization systems, mobile applications, and additional locales/currencies.
- Dedicated search/database/cache/queue infrastructure, service decomposition, orchestration platforms, and multi-region deployment.

### 4.3 Explicitly deferred

AI recommendations, advertising marketplaces, loyalty/subscriptions, multi-warehouse planning, cross-border commerce, live carrier integrations, and microservices are not prerequisites for the first trustworthy marketplace release. Reconsider each only with a named user need, an operating owner, success measures, and evidence that the current design cannot meet the requirement.

## 5. Functional requirements

Status legend: **Implemented** = current source has the capability; **Partial** = basic path exists but acceptance or policy gaps remain; **Open** = not implemented; **Deferred** = intentionally postponed pending evidence/decision. IDs are stable references for GitHub issues and pull requests.

### Identity, accounts, and permissions

- **FR-ID-01 — Partial:** A person can register and authenticate as a buyer or seller; public requests cannot grant administrator rights.
- **FR-ID-02 — Partial:** Users can end their session and manage their own account/profile data; sensitive actions require server-side authentication and permission checks.
- **FR-ID-03 — Open:** Registration email verification and account recovery must use verified, rate-limited, non-enumerating flows before a public launch. Recovery must safely invalidate or revoke existing sessions.
- **FR-ID-04 — Open:** Before a seller publishes public offers, onboarding records an approval state and permissioned review. This minimum approval is required for public seller onboarding; deeper verification and payout evidence are defined by the launch market, and payout data is collected only for an approved payout flow.
- **FR-ID-05 — Partial:** Role-to-operation behavior must remain documented and tested against the authorization matrix; account suspension and operator provisioning need a controlled workflow. Operators cannot grant themselves privileges, and suspension must block access immediately and revoke or invalidate existing sessions.

### Catalog, seller presence, and discovery

- **FR-CAT-01 — Partial:** Sellers can create and maintain their own product offers and stock; server authorization must enforce ownership.
- **FR-CAT-02 — Open:** Listings need draft/published/archived states. Only approved published offers appear in the public catalog; archiving must not erase order history.
- **FR-CAT-03 — Open:** Authorized operators can review, moderate, and disable unsafe or inaccurate offers with a recorded reason.
- **FR-CAT-06 — Open:** Authorized operators can manage a controlled product-category taxonomy; products must reference an allowed category.
- **FR-CAT-04 — Partial:** Product images must be safe, valid, reliably served, and have useful alternatives. Current URL-based media is not a managed media service.
- **FR-CAT-05 — Open:** Sellers have a store profile with buyer-visible business information and a controlled support contact.
- **FR-CAT-07 — Deferred:** Canonical product identity, seller offers, category-specific attributes/variants, and external product identifiers are introduced when the selected categories and catalog-quality evidence require them.
- **FR-SEARCH-01 — Partial:** Buyers can browse and apply current client-side filters/sorting. Server-side search must match product name and description and combine correctly with category filters.
- **FR-SEARCH-02 — Deferred:** Pagination and richer facets must be introduced when catalog size, usability, or measured response limits warrant them.
- **FR-SEARCH-03 — Deferred:** Dedicated indexing/ranking/personalization requires demonstrated search-quality or latency needs and an evaluation dataset.

### Basket, order, delivery, and money

- **FR-ORDER-01 — Implemented:** A buyer can maintain a basket and submit an order; price, stock, delivery fee, and total are calculated by the server.
- **FR-ORDER-02 — Partial:** Repeating the same checkout request after a timeout must not create duplicate orders or decrement stock twice; define and persist an idempotency contract.
- **FR-ORDER-03 — Partial:** Buyers can view order progress, cancel when policy/state allows, and confirm receipt; sellers can progress only their own order lines.
- **FR-ORDER-04 — Open:** Before market launch, order records and displays must consistently identify an ISO currency and avoid combining incompatible currencies.
- **FR-ORDER-05 — Open:** Support operators need permissioned case handling for delivery problems, cancellations, returns, and order correction, with a durable audit history.
- **FR-DEL-01 — Partial:** Current delivery choices are application-defined and seller fulfillment is manually updated. Buyer-facing promises must match a published, operationally supported policy.
- **FR-DEL-02 — Deferred:** Carrier quotes, labels, tracking webhooks, split shipments, and outsourced fulfillment require market and seller-operating decisions.
- **FR-PAY-01 — Deferred:** Collect payment only after the launch country, currency, provider availability, fee/tax responsibility, and checkout policy are approved.
- **FR-PAY-02 — Deferred:** Payment/refund processing must use hosted/tokenized provider flows, verified provider notifications, safe retries, auditable state transitions, and no raw card storage in Unikly.
- **FR-PAY-03 — Deferred:** Seller settlement, marketplace fees, reconciliation, and payout failure handling require an approved commercial model and permissioned ledger.

### Reviews, communication, and operator capabilities

- **FR-REV-01 — Partial:** Buyers can review a product only after the agreed eligible purchase completion state; reviews must state whether purchase verification was checked.
- **FR-REV-02 — Partial:** Administrators can moderate reviews; the seller must not be able to alter a buyer's review or private account information.
- **FR-SUP-01 — Open:** Buyers and sellers need a discoverable route to report an order or listing issue, receive a case status, and see next steps.
- **FR-NOTIF-01 — Open:** Important committed order and account state changes should produce safe, retryable transactional notifications without sending secrets or sensitive payment data.
- **FR-REPORT-01 — Open:** Sellers can see a basic summary of their own listings, units/orders, fulfillment state, and sales after the underlying payment/accounting definitions are agreed.
- **FR-ADMIN-01 — Open:** Authorized operators can review sellers and listings, resolve order cases, suspend accounts under policy, and audit sensitive actions.
- **FR-RISK-01 — Open:** Before accepting live payments, assign an owner and a documented process for provider risk signals, suspected abuse, chargebacks, and payment disputes.
- **FR-RISK-02 — Deferred:** Automated risk scoring and account, listing, or review abuse detection require market, privacy, and operating evidence, plus a process for reviewing and appealing automated decisions.

## 6. Non-functional requirements

- **NFR-SEC-01:** Deny API access by default; enforce least-privilege role permissions and resource ownership on the server. Protect mutations against CSRF and validate all untrusted input.
- **NFR-SEC-02:** Never store raw payment-card data, credentials, or provider secrets in source control, logs, or browser storage. Production secrets are managed outside the repository.
- **NFR-REL-01:** Retries of order and payment operations are safe and observable. State changes must be transactional or recoverable and auditable.
- **NFR-REL-02:** Production launch requires a documented database backup policy and a successful restore rehearsal with recorded recovery evidence.
- **NFR-OPS-01:** Production launch requires health monitoring, actionable alerts, structured operational logs, a deployment/rollback procedure, and an incident contact/runbook.
- **NFR-PERF-01:** Establish expected launch traffic and representative catalog/order data before setting a performance gate. Measure key browse, search, login, and order endpoints and record p95 results; do not claim a target without its test profile.
- **NFR-UX-01:** Buyer and seller flows work at supported narrow and desktop viewports and provide visible loading, empty, validation, and failure states.
- **NFR-A11Y-01:** Target [WCAG 2.2 AA](https://www.w3.org/TR/WCAG22/) for public launch, with semantic structure, keyboard operation, accessible names/status messages, sufficient contrast, visible focus, and reduced-motion support.
- **NFR-PRIV-01:** Define data collection, retention, export/deletion, consent, and support access according to the selected market before production launch.
- **NFR-PRIV-02:** By default, a seller's own order view exposes the recipient's name and delivery address needed to fulfill that seller's lines; it does not expose the buyer's email. Expose a phone number only when the approved fulfillment method requires it. Support may access additional contact details only for an authorized case, with access scoped to that case and audited.
- **NFR-MAINT-01:** Preserve a modular Spring monolith and Angular feature boundaries; make architecture changes only when an owned capability or measured constraint calls for it.

## 7. Core acceptance flows

The first complete marketplace release must verify these paths end to end, including role and ownership denials:

1. A buyer browses a published offer, views accurate seller/product information, adds it to a basket, and sees a server-calculated total.
2. A buyer places an order once; refresh/retry does not create a duplicate. Inventory cannot be oversold under concurrent requests.
3. A seller can see and fulfill only their own order lines; the buyer can follow the resulting state.
4. An unauthorized buyer/seller cannot view or mutate another account's private data, listings, basket, or seller order operations. A seller sees the recipient name and delivery address for their own lines, no email by default, and a phone number only when the approved delivery method needs it; broader support access is case-scoped and audited.
5. Registration is not presented as verified until email proof succeeds. Account recovery is rate-limited and non-enumerating, invalidates or revokes sessions safely, and cannot change another user's credentials.
6. A completed eligible buyer order can be reviewed under the stated review policy; ineligible and canceled orders cannot, and the verified-purchase marker matches the enforced rule.
7. Only an authorized operator can provision or suspend/reactivate accounts under policy; operators cannot grant themselves privileges. Suspension immediately blocks access and revokes or invalidates existing sessions. Every sensitive lifecycle action records actor, target, reason, and time.
8. A support operator can use explicitly permissioned tools to resolve launch-policy issues, with sensitive actions recorded.

Payment, refunds, returns, carrier integrations, and payouts add their own success, retry, failure, reconciliation, and access-control flows when those phases are approved.

## 8. Benchmark references

These official marketplace materials inform capability categories such as seller onboarding, product identifiers, fulfillment/returns, seller performance, and operating at scale. They are comparison inputs, not a commitment to copy another company's features or market model:

- [Walmart Marketplace overview and seller onboarding](https://marketplace.walmart.com/about-walmart-marketplace/)
- [Walmart seller registration guide](https://marketplace.walmart.com/seller-registration-guide/)
- [Walmart shipping and fulfillment](https://marketplace.walmart.com/shipping-and-fulfillment/)
- [Walmart Marketplace Pro Seller standards](https://marketplace.walmart.com/pro-seller/)
- [Amazon seller-fulfilled returns](https://sell.amazon.com/blog/manage-customer-returns)
- [Amazon 2025 Form 10-K](https://www.sec.gov/Archives/edgar/data/1018724/000101872426000004/amzn-20251231.htm)

## 9. Change control

Update this SRS when product scope, acceptance, or a deferred capability changes. Link implementation issues and pull requests to the stable FR/NFR IDs. Record active increment state in [the implementation roadmap](product-roadmap.md), release evidence in [release readiness](release-readiness.md), and completed verified outcomes in the root `CHANGELOG.md`. Do not use the obsolete freelance-services roadmap in `UNIKLY_BUILD_AND_ONBOARDING_GUIDE.md` to scope this product.
