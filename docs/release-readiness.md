# Delivery tracking and release readiness

## Progress tracking

- [docs/UNIKLY_SRS_v4.md](UNIKLY_SRS_v4.md) is the active requirements and capability-status baseline.
- [docs/product-roadmap.md](product-roadmap.md) is the shared sequence and current product-progress view. Keep milestone state and unresolved product decisions there; do not duplicate its full backlog here.
- Use GitHub issues from the [work-item template](../.github/ISSUE_TEMPLATE/work-item.yml) for open increments, defects, and release gaps. Keep requirements, acceptance criteria, and verification evidence on the issue.
- Use pull requests to show implementation progress. The [pull request template](../.github/pull_request_template.md) records affected modules, checks, documentation updates, and unresolved gaps.
- Use [CHANGELOG.md](../CHANGELOG.md) for completed increments and verified results. It is a history, not the active backlog.
- Keep [architecture.md](architecture.md) and [authorization-matrix.md](authorization-matrix.md) aligned with code. CI checks builds, backend tests, and module boundaries; reviewers check the remaining documentation and security behavior.

## Current release gates

| Gate | Status | Completion evidence |
| --- | --- | --- |
| Initial customer segment, launch region/currency, and operating promise | Open product decision | Decision record linked from the roadmap; needed before market-specific payment, tax, delivery, and payout behavior |
| Backend tests and module-boundary rules | Pending first successful CI run for this change | Green `backend` job in `.github/workflows/ci.yml` |
| Angular production build | Pending first successful CI run for this change | Green `frontend` job in `.github/workflows/ci.yml` |
| Required CI checks on the protected branch | Open repository setting | Require both CI jobs before merging to the default branch |
| Authentication, review policy, catalog, and checkout smoke flow | Pending after this change | Run the affected local Compose flow using `.agents/skills/e2e-smoke-testing/SKILL.md` |
| First-market go-live gate (M28) | Blocked by roadmap dependencies | Record evidence for SRS core flows: published offer to order, seller-only fulfillment, buyer tracking, cross-account denial, eligible/ineligible reviews, verified registration/recovery, seller approval, operator lifecycle, support, and responsive buyer/seller flows. Also require the gates below and NFR-UX-01/NFR-A11Y-01 review. |
| Hosted payment, retry, notification verification, and reconciliation (M20–M22) | Blocked on market/currency and prior roadmap work | Provider sandbox evidence for paid/declined states, retry safety, duplicate/out-of-order notifications, and reconciliation. Never store raw card data. |
| Provider risk signals, fraud escalation, chargebacks, and disputes (M1, FR-RISK-01) | Open product/operations decision | Name the response owner and document the manual review/escalation process before accepting live payments. Automated decisions remain deferred to M29/FR-RISK-02 and require a documented review/appeal process before enablement. |
| Refunds, marketplace fee ledger, and seller payout (M23–M25) | Blocked on approved commercial model and prior roadmap work | Auditable provider refund results and retries, reproducible seller balances, payout success/failure reconciliation, and seller-visible outcomes. |
| Manual delivery, support, and returns (M17, M26) | Blocked on launch policy and prior roadmap work | Approved region-specific policy, case and seller procedures, order state evidence, and return request/authorization/receipt flow. Paid monetary refunds use M23. |
| Optional pilot follow-ups | Deferred by evidence | M19 seller reports and M27 carrier/fulfillment integration are not M28 launch blockers when the manual pilot meets its promise; add them only with measured need and an owner. |
| Broader Angular browser-flow coverage | Open before a wider customer release | Add focused browser/component coverage for authentication, catalog/cart/checkout, and seller workflows, then run it in pinned Node/Chromium CI |
| Storefront trust-copy and keyboard/accessibility audit | Open before a public customer release | Validate delivery/return/payment claims against policy; keyboard-test category filter and review picker; record browser evidence |
| Responsive visual/accessibility review | Open before a public customer release | Browser evidence for narrow/mobile and desktop viewports, contrast, focus, reduced motion, and image failure behavior; see [theme-style-audit.md](theme-style-audit.md) |
| PostgreSQL 18 production backup and restore rehearsal | Open before production launch | Recorded restore rehearsal and recovery evidence |
| Production monitoring, alerting, TLS, and secret configuration review | Open before production launch | Deployment review and operational runbook |

Do not mark a gate complete from source changes alone. Record the command, environment, and result in the issue or pull request, then summarize verified work in the changelog.
