# Unikly — Build & Onboarding Guide

**Companion to:** Unikly Software Requirements Specification, v2.0
**Audience:** Anyone building on Unikly — today or after joining later
**Maintained by:** Aboubakar Garba Faouzi — Project Owner, Product Lead & Architect

---

## How to use this document

Read the SRS first — it defines *what* the platform must do and *why*, phase by phase. This guide defines *how* we build it, in what order, and what "done" means at each stage. Every task below is tagged with the FR/NFR IDs it satisfies, so if you're ever unsure why a step exists, trace it back to the SRS.

**The rule that governs everything else: we build small and prove it before we build big.** Do not start a Phase N+1 task while a Phase N "Must" item is unfinished or unstable. Do not introduce Kafka, microservices, or AI matching early because they sound more impressive — they show up in Phase 3 and 4 for a reason: the earlier phases generate the data and prove the demand that make them worth the complexity.

Each phase ends with **exit criteria**. Exit criteria mean a working demo, not "the code is merged." If you can't demo it end-to-end, the phase isn't done.

---

## Guiding principles

1. **Modular monolith before microservices.** Phases 1–3 ship as one Spring Boot application, internally organized by the same domain boundaries (user, job, matching, payment, messaging, notification, search) that become real services in Phase 4. This makes the eventual split a refactor, not a rewrite.
2. **Every phase ships something a real user could use.** No phase is purely internal plumbing.
3. **Data before intelligence.** Matching and analytics (Phase 3) wait until Phases 1–2 have produced real jobs, proposals, and completed transactions to learn from.
4. **Boring infrastructure choices, deferred until they're needed.** Kafka, Kubernetes, and multi-region anything are Phase 4/5 concerns. Don't pay that complexity tax on day one.
5. **Traceability.** Every build task references the FR/NFR ID it implements. If a task doesn't trace back to the SRS, question whether it belongs in this phase.

---

## Phase 0 — Project Setup & Foundations

**Goal:** A new contributor can clone the repo and be productive in under an hour.

- [ ] Set up the repository: `/backend` (Spring Boot monolith), `/frontend` (Angular), `/docs` (this guide, the SRS, and any architecture decision records).
- [ ] Inside `/backend`, create empty domain packages up front: `identity`, `profile`, `job`, `matching`, `payment`, `messaging`, `notification`, `search` — even though only a few have real logic in Phase 1. Treat `identity` as authentication infrastructure owned by the User/Profile context, not as a ninth service to extract later.
- [ ] Stand up local Docker Compose with PostgreSQL, Keycloak, and MinIO from day one, even though the app itself is a single deployable. Getting infrastructure parity right early avoids surprises later.
- [ ] Adopt Flyway (or an equivalent) for schema migrations from the first commit — never hand-edit the schema.
- [ ] Agree on a lightweight git workflow: `main` is always deployable; work happens on short-lived feature branches; every change goes through a pull request and a self- or peer-review, even solo.
- [ ] Define environment configuration via Spring profiles (`dev`, `staging`, `prod`) and a `.env`-style local override — no secrets committed, ever.
- [ ] Write the repo `README.md`: how to run the stack locally, how to run tests, where the SRS and this guide live.
- [ ] Agree on the Definition of Done template used at the end of every phase (see each phase below) and keep it visible in the repo.

**Exit criteria:** `docker compose up` plus one documented command gets a new contributor a running stack with Keycloak, Postgres, and MinIO, and the empty Spring Boot skeleton compiles and starts.

---

## Phase 1 — Foundation (MVP)

**Goal:** A client can post a job, a freelancer can propose, they can message, and the job can be marked complete manually — with no payments, no matching, no async infrastructure. This is the smallest version of the platform that is genuinely useful.

**Build order:**

- [ ] Integrate Keycloak: realm and client setup, Authorization Code + PKCE flow wired into the Angular app, JWT validation filter in Spring Boot. *(FR-AUTH-01, FR-AUTH-02, FR-AUTH-04)*
- [ ] Build registration and login screens, including role selection at signup (Client or Freelancer). *(FR-AUTH-01)*
- [ ] Build password-reset-via-email flow. *(FR-AUTH-03)*
- [ ] Build profile CRUD: freelancer skills/bio/rate/portfolio; client name/company/description. *(FR-PROF-01, FR-PROF-02)*
- [ ] Build job posting CRUD (title, description, category, budget, deadline) plus browse/filter listing and keyword search across job titles and descriptions. *(FR-JOB-01, FR-JOB-02, FR-JOB-03, FR-SEARCH-01)*
- [ ] Build proposal submission (cover letter, bid, delivery estimate) and the client-side review/accept flow that assigns the job. *(FR-MATCH-01, FR-MATCH-02)*
- [ ] Build one-to-one messaging tied to a specific job, including file attachments uploaded to MinIO. Polling refresh is fine here — real-time comes in Phase 3. *(FR-MSG-01, FR-MSG-02)*
- [ ] Build a manual "mark complete" action plus a basic 1–5 star rating capture. This is a deliberately lightweight placeholder ahead of the full review system in Phase 2.
- [ ] Build the responsive Angular shell with role-based route guards. *(NFR-UX-01, NFR-SEC-05)*
- [ ] Start the internationalization foundation in Phase 1 and ship English and French for all user-facing Phase 1 flows. *(NFR-I18N-01)*
- [ ] Apply clear, jargon-free copy and test the registration, job, proposal, messaging, and completion flows against WCAG 2.1 AA criteria. *(NFR-UX-02, NFR-UX-03)*
- [ ] Enforce HTTPS, input validation, and parameterized queries as house rules from the first line of backend code. *(NFR-SEC-01, NFR-SEC-02, NFR-SEC-03)*
- [ ] Define and verify the Phase 1 performance budget: standard read endpoints meet the 95th-percentile 500 ms target at expected launch load. *(NFR-PERF-01)*
- [ ] Encrypt personal data at rest and document retention periods for accounts, messages, portfolio files, and completed transactions. *(NFR-COMP-01)*
- [ ] Write automated tests for the auth flow and the job/proposal happy path. *(NFR-MAINT-02)*
- [ ] Set up scheduled daily database backups with documented retention, restore verification, and recovery targets. *(NFR-REL-03)*

**Exit criteria:** In a demo environment, a client registers, posts a job, receives and accepts a proposal, exchanges messages with the freelancer, and marks the job complete — start to finish, no manual database intervention. Every Phase 1 "Must" requirement in the SRS is satisfied.

---

## Phase 2 — Trusted Transactions

**Goal:** Money moves safely through the platform, jobs have a real lifecycle instead of a single "complete" button, and admins have a way to intervene.

**Build order:**

- [ ] Implement the job state machine as an explicit, testable component: `DRAFT → OPEN → IN_REVIEW → IN_PROGRESS → COMPLETED → CLOSED`, with `CANCELLED`, `DISPUTED`, and `REFUNDED` branch states. Replace the Phase 1 manual "mark complete" with real state transitions. *(FR-JOB-04)*
- [ ] Add a Milestone entity and UI so longer jobs can be split into budgeted, dated chunks. *(FR-JOB-05)*
- [ ] Add a two-step confirmation flow for edits to an in-progress job, requiring both parties to agree before the change takes effect. *(FR-JOB-06)*
- [ ] Integrate Stripe Connect: freelancer payout onboarding (Connect Express), client funding into escrow, and release-on-approval logic tied to the state machine. *(FR-PAY-01, FR-PAY-02, FR-PAY-03)*
- [ ] Implement automatic platform commission calculation at release time. *(FR-PAY-05)*
- [ ] Validate the Stripe Connect charge, transfer, escrow-like hold, payout, refund, and country-availability model against the target launch markets before enabling live payments. *(FR-PAY-01, FR-PAY-02, FR-PAY-03)*
- [ ] Build the dispute flag (either party can raise one, freezing escrow) and an admin dispute queue with manual release/refund power. *(FR-PAY-04, FR-ADMIN-03)*
- [ ] Add Google sign-in through Keycloak and an optional freelancer identity-document verification flow with a visible Verified badge. *(FR-AUTH-05, FR-PROF-03)*
- [ ] Build written reviews tied to job completion, visible on public profiles and rolled into an average rating. *(FR-REV-01, FR-REV-02)*
- [ ] Migrate the Phase 1 star rating into the Phase 2 review record, preserving its author, job, timestamp, and score; do not double-count it in averages. *(FR-REV-01, FR-REV-02)*
- [ ] Build Admin Console v1: suspend/ban users, manage job and skill categories. *(FR-ADMIN-01, FR-ADMIN-02)*
- [ ] Wire transactional email for the key lifecycle events: proposal received, job assigned, payment released, new message. *(FR-NOTIF-01)*
- [ ] Add a clear consent step, Terms of Service / Privacy Policy acceptance at signup, and a basic data access/delete request handler. *(NFR-COMP-01, NFR-COMP-02)*
- [ ] Extend automated test coverage specifically to the state machine and payment calculations — this is the code that must not silently break. *(NFR-MAINT-02)*
- [ ] Verify the responsive transactional flows against WCAG 2.1 AA before declaring the paid-job demo complete. *(NFR-UX-02)*
- [ ] Ensure raw card data never touches the backend — Stripe Elements/Checkout only. *(NFR-SEC-04)*

- [ ] Measure and document availability for the core login, job posting, and payment flows against the 99.5% target before completing the phase. *(NFR-REL-01)*

**Exit criteria:** A full paid job runs end-to-end — escrow funded, milestone released, commission deducted, review left by both sides — and an admin can resolve a simulated dispute by hand. Core transactional flows meet the 99.5% availability target and pass backup/restore checks.

---

## Phase 3 — Intelligence & Discovery

**Goal:** The platform gets smarter and faster to use, now that Phases 1–2 have produced real jobs, proposals, and completed-job history to learn from.

**Build order:**

- [ ] Stand up a search index (OpenSearch or Elasticsearch) and index Job and Profile documents for full-text and faceted search. *(FR-SEARCH-02, FR-SEARCH-03)*
- [ ] Build hybrid matching engine v1: start heuristic/rules-based (skill and category overlap, weighted by completed-job performance and review history) rather than a trained model — there isn't enough data yet for the latter to beat a well-tuned heuristic. *(FR-MATCH-03, FR-REV-03)*
- [ ] Add proactive match notifications for freelancers whose profile clears a relevance threshold on a new job. *(FR-MATCH-04)*
- [ ] Add a profile-completeness indicator and include it in matching-quality instrumentation. *(FR-PROF-04)*
- [ ] Replace polling messaging with real-time delivery over WebSocket/STOMP, plus read receipts and typing indicators. *(FR-MSG-03, FR-MSG-04)*
- [ ] Build the in-app notification center with an unread badge. *(FR-NOTIF-02)*
- [ ] Build freelancer earnings/completion analytics and client spend/hiring analytics dashboards. *(FR-REPORT-01, FR-REPORT-02)*
- [ ] Build the admin platform-health dashboard (GMV, active users, dispute rate) and CSV export. *(FR-ADMIN-04, FR-REPORT-03)*
- [ ] Introduce structured, centralized logging across every module of the monolith — this is a rehearsal for the service boundaries coming in Phase 4. *(NFR-OBS-01)*

**Exit criteria:** A manual review of matching results on real Phase 1–2 data shows the ranked shortlist meaningfully outperforms plain keyword browsing. Real-time messaging and notifications hold up under normal load. Logging is consistent across every module.

---

## Phase 4 — Distributed Architecture

**Goal:** Split the modular monolith into independently deployable services, one bounded context at a time, without breaking anything Phases 1–3 built.

**Recommended extraction order** (least-coupled first): User Profile → Job → Messaging → Payment → Matching → Notification → Search.

**Build order (repeat per service):**

- [ ] Stand up Kafka in KRaft mode and an API Gateway in the local Docker Compose environment before extracting the first service.
- [ ] For each bounded context in turn: give it its own database/schema, define the events it publishes and the events it consumes, extract it into its own Spring Boot service, replace in-process calls with REST (synchronous queries) or Kafka events (state changes), and update gateway routing.
- [ ] Add Resilience4j circuit breakers and retry policies at every new service-to-service boundary as it's created. *(NFR-REL-02)*
- [ ] Stand up centralized observability — Prometheus/Grafana dashboards and Sentry error tracking — and wire each service into it as it's extracted. *(NFR-OBS-02, NFR-OBS-03)*
- [ ] Re-run the full Phase 1–3 regression suite against the distributed system after every single extraction, not just at the end. A service is not "done" until the old monolith test suite passes against the new topology.

**Exit criteria:** All seven bounded contexts run as independent services behind the gateway. The Phase 1 demo flow and the Phase 2 paid-job flow both still work end-to-end, now across service boundaries. Dashboards show live health, latency, and error metrics for every service. *(NFR-PERF-02)*

---

## Phase 5 — Growth & Scale

**Goal:** The platform is ready for more users, more markets, and new account types. This phase is opportunity-driven — pull items in as real demand justifies them, rather than building all of it speculatively.

- [ ] Multi-currency support in the Payment service. *(FR-PAY-06)*
- [ ] Agency/Team account model on top of the User Profile service, with member rosters and job routing.
- [ ] Formal dispute/arbitration workflow beyond basic admin review.
- [ ] Mobile app groundwork: API stability review, push notification provider integration. *(FR-NOTIF-03)*
- [ ] Add AI-assisted proposal drafting suggestions only after the matching and proposal data has enough quality for evaluation. *(FR-MATCH-05)*
- [ ] Horizontal scaling / Kubernetes manifests per service. *(NFR-PERF-02, NFR-PERF-03)*
- [ ] Add locale-aware currency/date formatting and additional languages as new markets open. *(NFR-I18N-02)*
- [ ] Multi-factor authentication for high-value accounts. *(FR-AUTH-06)*
- [ ] Independent security review / penetration test ahead of any enterprise sales push. *(NFR-SEC-06)*
- [ ] Complete a formal compliance review before pursuing enterprise clients. *(NFR-COMP-03)*

**Exit criteria:** Defined per initiative — this phase doesn't have one finish line, each item has its own.

---

## Working agreements for anyone joining later

- Read the SRS first, then this guide, before writing code.
- Never start a Phase N+1 task while a Phase N "Must" item is unfinished or unstable — check the phase's exit criteria, not just the task checkboxes.
- "Done" means a working demo against the exit criteria, not a merged pull request.
- If reality diverges from this plan — and it will — update this guide and the SRS together. They're living documents, not a contract carved in stone; the phase *order* and the *start-small* discipline are the parts that shouldn't be renegotiated mid-phase.
