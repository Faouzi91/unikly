# Unikly implementation roadmap and progress ledger

**Last reviewed:** 2026-10-20
**Product requirements:** [UNIKLY_SRS_v4.md](UNIKLY_SRS_v4.md)
**Release evidence:** [release-readiness.md](release-readiness.md)

This is the shared product-progress view for developers and coding agents. The SRS defines expected behavior; this file sequences it; GitHub issues are the active work queue; pull requests hold acceptance and verification evidence; `CHANGELOG.md` records only completed, verified changes. Update this page when a roadmap increment changes state or a decision changes scope.

## Product status

Unikly has a working early marketplace foundation: account sessions and permissions, persistent catalog and cart, stock-aware order creation, buyer order history, manual seller fulfillment, and product reviews. It is not yet a production-ready or full-scale marketplace. Current source and automated checks do not establish launch readiness. Payment collection, carrier integration, returns/refunds, seller settlements, seller storefront identity, managed listing/category operations, and production operating procedures remain incomplete.

### Decisions needed before market-specific commerce

| Decision | Owner | State | Needed before |
| --- | --- | --- | --- |
| First buyer segment and seller wedge/category | Product owner | Open | Public positioning and catalog acceptance |
| Initial country/region and supported currency | Product/operations owner | Open | Currency, payment, tax, privacy, and payout design |
| Buyer-facing delivery and return promise | Operations owner | Open | Publishing delivery/return trust claims |
| Minimum seller review/approval before public offers | Product/operations owner | Open | Public seller onboarding |
| Marketplace commercial model (fees, seller payout, refund responsibility) | Product/finance owner | Open | Live payments and seller settlement |
| Payment disputes, provider risk signals, fraud escalation, and chargeback responsibility | Product/finance/operations owner | Open | Accepting live payments |
| Launch support coverage and service hours | Operations owner | Open | Public launch and escalation design |
| Target traffic profile and service objectives | Product/engineering/operations | Open | Performance and availability gates |

These decisions do not block reversible improvements such as search correctness, listing ownership, accessibility semantics, or documentation alignment. Record each decision and rationale in its linked issue before using it to constrain architecture.

## Delivery sequence

Statuses: **Done** requires acceptance and required verification evidence in the issue/PR, a changelog entry, and a pushed topic-branch commit. **Accepted (commit/push pending)** means acceptance and checks are complete but the requested Git handoff is unfinished. **Ready** means scoped and can be started; **Blocked** means a named decision/dependency is missing; **Planned** means sequenced but not yet fully refined. Do not infer completion from source presence alone.

| ID | Increment | Status | SRS | Exit evidence |
| --- | --- | --- | --- | --- |
| M0 | Reconcile product truth, create a shared SRS/roadmap, and synchronize agent instructions | Done | All | Documentation references agree; requirement status separates code presence from release proof; diff/link review recorded in this changelog and pushed on the topic branch. |
| M1 | Record first customer/seller wedge, launch market, and delivery promise | Blocked on product decisions | §1, §4, FR-DEL-01 | Signed decision record identifies segment, region, category, currency candidate, and policy owner. |
| M2 | Remove or rewrite unsupported storefront delivery, returns, and payment claims | Ready | FR-DEL-01, FR-PAY-01 | Each displayed promise is backed by an implemented flow and published policy; demo checkout clearly explains whether it collects payment. |
| M3 | Restrict reviews to the agreed completed-purchase state | Ready | FR-REV-01 | Placed/processing/canceled orders cannot qualify; the agreed completed state can; the verified marker matches the rule. |
| M4 | Identify seeded/demo listings and seller provenance | Ready | FR-CAT-01, FR-CAT-02 | Every fixture shown in a public environment is visibly demo content or attributed to a real seller; no offer implies false verification. |
| M5 | Minimize buyer contact data visible in seller order views | Ready | NFR-PRIV-02 | Seller sees recipient name/address for own lines, no buyer email by default, and phone only if the approved delivery method needs it. Case-scoped support access is delivered and audited in M17. |
| M6 | Add seller store profiles and buyer-visible seller identity | Planned | FR-CAT-05 | Seller can update own public profile; buyer can view it; private seller data remains protected. |
| M7 | Add listing draft/publish/archive lifecycle | Planned | FR-CAT-02 | Sellers can transition their own offers; public browse shows only published listings; order history remains readable after archive. |
| M8 | Add minimum permissioned listing moderation using the initial category set | Planned | FR-CAT-03, FR-ADMIN-01 | Authorized operators can review/disable an offer; denied roles cannot; action and reason are recorded. Do not add category administration in this slice. |
| M9 | Make search server-backed for product name and description | Planned | FR-SEARCH-01 | Description-only matches work, query validation is bounded, and empty/error/loading states are clear. |
| M10 | Add an operator-managed category source and server-validated category filtering | Planned | FR-CAT-06, FR-SEARCH-01 | Operators manage allowed categories; sellers cannot invent invalid categories; buyer filters combine with search and selected state follows the agreed URL behavior. |
| M11 | Validate product media and provide reliable image behavior | Planned | FR-CAT-04 | Invalid/unavailable media fails safely; upload/source rules, file constraints, fallback, and authorization are verified. |
| M12 | Make order creation retry-safe | Planned | FR-ORDER-02, NFR-REL-01 | Replaying one request returns the original order and one stock decrement; a distinct request can place a separate order. |
| M13 | Decide and store one launch currency consistently | Blocked on M1 | FR-ORDER-04 | Approved ISO currency is carried through product/order/delivery amounts and formatting; incompatible amounts cannot be combined. |
| M14 | Add verified registration email and account recovery | Planned | FR-ID-03 | Registration requires email proof before the account is treated as verified. Recovery is rate-limited, non-enumerating, and safely invalidates/revokes sessions. |
| M15 | Add seller verification and controlled onboarding state | Planned | FR-ID-04 | Seller status is visible to the seller and authorized operators; verification/payout evidence is private and access-audited. Seller publication is denied until the required approval state is reached. |
| M16 | Add permissioned operator account lifecycle controls | Planned | FR-ID-05, FR-ADMIN-01 | Only authorized operators can provision, suspend, and reactivate accounts under policy; operators cannot grant themselves privileges. Actor, target, reason, and time are audited; suspension immediately blocks access and revokes or invalidates existing sessions. |
| M17 | Add permissioned support cases for order/listing problems | Planned | FR-ORDER-05, FR-SUP-01, FR-ADMIN-01, NFR-PRIV-02 | Customer can report an issue and see status; support resolves it under least privilege. Additional buyer contact data is available only within an authorized case and every access/action is audited. This slice does not execute a monetary refund. |
| M18 | Add transactional order notifications | Planned | FR-NOTIF-01 | Key committed order transitions produce retry-safe messages with no secret or payment data exposure. |
| M19 | Add seller-scoped operating reports | Planned; may follow first pilot | FR-REPORT-01 | Seller sees only their own order/fulfillment summary with clear definitions and reproducible totals. |
| M20 | Complete one hosted-provider payment success path in sandbox | Blocked on M1, M12, M13 | FR-PAY-01, FR-PAY-02 | One buyer order reaches a verified paid state in provider sandbox; Unikly stores provider references/state only and never raw card data. |
| M21 | Handle provider-declined and transient payment failures safely | Blocked on M20 | FR-PAY-02, NFR-REL-01 | A failed payment leaves a clear recoverable order/payment state; buyer retries cannot create a duplicate charge or order transition. |
| M22 | Verify provider notifications and reconcile asynchronous payment state | Blocked on M20–M21 | FR-PAY-02, NFR-REL-01 | Invalid, replayed, duplicated, late, and out-of-order notifications are handled safely; reconciliation exposes mismatches for operator action. |
| M23 | Add auditable partial/full refunds | Blocked on M1, M17, M22 | FR-PAY-02 | Authorized case can request/record a refund; provider result, retry/failure, order state, and audit history reconcile without double refund. |
| M24 | Record the approved marketplace fee against paid orders | Blocked on M1 and M20 | FR-PAY-03 | Each eligible paid order gets a reproducible fee/seller balance entry from the approved terms; corrections are auditable and do not rewrite payment history. |
| M25 | Settle seller balances and reconcile payout outcomes | Blocked on M1 and M24 | FR-PAY-03 | Permissioned payout flow handles success, failure, retry, reconciliation, and seller-visible status against approved commercial terms. |
| M26 | Run the approved manual delivery and returns operating model | Blocked on M1, M17, and M23 for paid returns | FR-DEL-01, FR-ORDER-05 | Published policy, support process, seller obligations, and order state transitions work for the launch region. This covers return request/authorization/receipt; monetary refunds use M23. No carrier API is assumed. |
| M27 | Add carrier/fulfillment integration only when the pilot proves it is needed | Deferred; depends on M1 and M26 | FR-DEL-02 | Named provider/contract, volume or service evidence, operational owner, failure handling, and measurable benefit justify integration. |
| M28 | Pass first-market production-readiness and go-live gates | Blocked on M1, M2–M18, M20–M26 | NFR-SEC-01..02, NFR-REL-02, NFR-OPS-01, NFR-PERF-01, NFR-UX-01, NFR-A11Y-01, NFR-PRIV-01..02, FR-RISK-01 | End-to-end verify browse-to-order, seller fulfillment limited to own order lines, buyer status tracking, and cross-account/cross-seller denials (SRS flows 1–4); eligible completed-purchase and ineligible review attempts with a matching verified marker (M3); verified registration/recovery (M14); seller approval (M15); operator provisioning/suspension/reactivation (M16); support (M17); and notifications (M18). Verify responsive buyer and seller flows at narrow and desktop widths. Verify payment/retry/reconciliation (M20–M22), refund/fees/settlement (M23–M25), and delivery/return/support (M17, M26). Before live payments, approve provider risk-signal, fraud escalation, chargeback, and dispute ownership/process (M1, FR-RISK-01). Complete restore rehearsal, TLS/secrets, alerts/runbooks, privacy/security review, WCAG 2.2 AA review, and measured performance. Operational preparation may proceed while these flows are built. M27 is optional if the manual pilot meets the promise; M19 seller reports may follow the first pilot. |
| M29 | Expand capabilities from usage evidence | Deferred | FR-CAT-07, FR-SEARCH-02..03, FR-DEL-02, FR-RISK-02, NFR-MAINT-01 | An issue states user evidence, operating owner, success measure, and why the current system is insufficient before adding canonical product/offer identity, category-specific variants, automated fraud/abuse controls, richer search, bulk feeds, promotions, complex logistics, mobile, or service decomposition. Before automated decisions are enabled, provide a documented review and appeal process. |

## Theme and storefront workstream

See the detailed [theme and style audit](theme-style-audit.md). Keep visual work incremental: trust/semantics work supports M2–M4, while public-release visual evidence is part of M28:

1. Fix truthful trust messages and accessible control semantics.
2. Introduce shared design tokens and reduced-motion support; migrate the shell, storefront, and catalog first.
3. Bring cart, checkout, identity, account, and seller controls onto the same button, form, spacing, focus, and status patterns.
4. Improve image reliability, page titles/metadata, touch target sizes, and content hierarchy.
5. Verify keyboard use, contrast, reduced motion, and 320/375px through desktop layouts in a real browser.

Do not schedule a broad visual redesign before the token and semantics pass yields evidence. A reviewer should check actual screenshots or browser dimensions, not only infer the rendered result from CSS.

## How to deliver and track each increment

1. Open one GitHub issue for a small vertical slice. Include its roadmap ID, SRS IDs, user outcome, explicit non-goals, acceptance checklist, verification commands/flows, dependencies, and decision owner.
2. Keep one user-visible outcome per topic branch and pull request. Avoid combining unrelated roadmap increments.
3. The primary delivery session reads the issue, this roadmap, the SRS, architecture, and authorization matrix before delegating. Give every specialist the same issue/SRS IDs, roadmap state, changed paths, and a narrow review question. Specialists are read-only; the primary integrates findings and owns truth updates.
4. Update status only when the issue/PR contains evidence. Build/test output is not live-flow evidence; a planned test is not a passed test. For behavior changes, use the pinned Docker workflows and applicable Compose smoke flow in the skills.
5. Before a milestone is complete, review the final diff, update relevant architecture/authorization/release docs, and add a dated CHANGELOG entry with exactly the checks that passed and any open gate.
6. After a milestone passes its stated acceptance and verification, commit it on its topic branch using a Conventional Commit and push that branch to `origin`. Do not push to the default branch, force-push, or claim completion with a blocking acceptance check outstanding. Keep `.env`, credentials, build output, and unrelated work out of the commit.
7. On the next increment, refresh the issue/branch context from the latest roadmap and shared source files so Codex, Gemini, and other contributors begin from the same project state.

## Current evidence boundary

The older SRS v3 describes an earlier, much smaller product snapshot and is not the active roadmap. Current code can be ahead of or behind this plan on any given branch. Use the checked-out source and tests for code facts, GitHub issues/PRs for live increment state, this roadmap for sequencing, and the release gate table for launch evidence. Do not treat this table as proof that a code path is currently deployed.
