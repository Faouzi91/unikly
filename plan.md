# Unikly — Project Plan & Feature Status

> Generated: 2026-03-22 · Based on SRS v1.0 cross-referenced with current codebase
> Stack: Angular 19 · Spring Boot 3.4 · Keycloak 26 · Kafka · Elasticsearch · PostgreSQL · Stripe · Docker

---

## 1. Architecture Overview

Unikly is an AI-powered freelancing platform with event-driven microservices architecture.

| Service | Port | Database | Status |
|---|---|---|---|
| API Gateway | 8080 | Redis | ✅ Running |
| User Service | 8082 | PostgreSQL (5434) | ✅ Running |
| Job Service | 8081 | PostgreSQL (5433) | ✅ Running |
| Payment Service | 8083 | PostgreSQL (5435) | ✅ Running |
| Matching Service | 8084 | PostgreSQL (5436) | ✅ Running |
| Messaging Service | 8085 | PostgreSQL (5437) | ✅ Running |
| Notification Service | 8086 | PostgreSQL (5438) | ✅ Running |
| Search Service | 8087 | Elasticsearch | ✅ Running |
| Matching AI | 8090 | — | ✅ Running (FastAPI + sentence-transformers) |

---

## 2. Bug Fixes Applied (2026-03-22)

### BUG-1 ✅ FIXED · Profile page 404
**Root cause**: `UserProfileService.getProfile()` threw `EntityNotFoundException` when no DB row existed (e.g. test users seeded directly in Keycloak never went through the `/api/users/register` flow).
**Fix**: Added `getOrCreateProfile(UUID userId, Jwt jwt)` method. On first access, auto-creates a minimal profile using JWT claims (`given_name`, `family_name`, `realm_access.roles`). No more 404 on first visit.
**Files**: `UserProfileService.java`, `UserProfileController.java`, `UserRole.java` (added `ADMIN` value)

### BUG-2 ✅ FIXED · Freelancers tab shows zero results
**Root cause**: `RegistrationService.createUserProfile()` saved to DB but never published a Kafka/outbox event → search-service never indexed the profile in Elasticsearch.
**Fix**: `RegistrationService` now injects `OutboxRepository` + `ObjectMapper` and publishes `UserProfileUpdatedEvent` after profile creation (same event as `UserProfileService.createOrUpdateProfile()`).
**Files**: `RegistrationService.java`

### BUG-3 ✅ FIXED · Payments page 500 error
**Root cause**: `PaymentService.getAllPayments()` called `GET /api/v1/payments` with no `jobId`. The backend only had `getPaymentsByJob(@RequestParam UUID jobId)` — missing required param → 400/500.
**Fix**: New `GET /api/v1/payments/mine` endpoint returns all payments where `clientId = userId OR freelancerId = userId`. Frontend `getAllPayments()` updated to call `/v1/payments/mine`.
**Files**: `PaymentController.java`, `PaymentService.java`, `PaymentRepository.java`, `payment.service.ts`

### BUG-4 ✅ FIXED · Job creation 405 Method Not Allowed
**Root cause**: Gateway route `Path=/api/v1/jobs/**` did NOT match `POST /api/v1/jobs` (no trailing path segment).
**Fix**: Predicate updated to `Path=/api/v1/jobs,/api/v1/jobs/**`.
**Files**: `backend/gateway/src/main/resources/application.yml`

### BUG-5 ✅ FIXED · Registered users get no Keycloak role
**Root cause**: `RegistrationService.createKeycloakUser()` created the user in Keycloak but never assigned the realm role. JWT contained no roles → `hasRole('ROLE_ADMIN')` always false.
**Fix**: Added `assignRealmRole(adminToken, userId, roleName)` that calls Keycloak's role-mapping API after user creation.
**Files**: `RegistrationService.java`

---

## 3. Full Feature Status Matrix (SRS Cross-Reference)

### 3.1 Backend Services

| Feature | SRS Section | Status | Notes |
|---|---|---|---|
| Keycloak realm + roles (CLIENT, FREELANCER, ADMIN) | 5.2.1 | ✅ | unikly-realm.json seeded |
| Registration → Keycloak user + role + DB profile | 5.2.2 | ✅ | BUG-2, BUG-5 fixed |
| JWT login + refresh token endpoint | 5.2.2 | ✅ | AuthenticationController |
| Social OAuth (Google/GitHub) | 5.2.2 | ✅ | Social code exchange endpoint |
| User profile CRUD (`/api/users/me`, `/api/users/{id}`) | 5.3.2 | ✅ | BUG-1 fixed (auto-create) |
| Freelancer search by skill (`GET /api/users?role=FREELANCER`) | 5.3.2 | ✅ | Direct DB query, no ES |
| User reviews (POST + GET) | 5.3.2 | ✅ | Backend complete; ReviewController |
| `UserProfileUpdatedEvent` via outbox | 5.3.3 | ✅ | On profile save + on registration |
| Job CRUD + lifecycle state machine | 5.4.1 | ✅ | DRAFT→OPEN→IN_PROGRESS→COMPLETED→CLOSED |
| Proposals (submit/accept/reject) | 5.4.3 | ✅ | ProposalController |
| Contract creation on proposal accept | 5.4.3 | ⚠️ Partial | Proposal accepted; contract entity TBD |
| `JobCreatedEvent` + outbox | 5.4.4 | ✅ | Triggers matching + search + notifications |
| Kafka consumer: `PaymentCompletedEvent` → job FUNDED | 5.4.5 | ✅ | PaymentEventConsumer |
| AI matching (rule-based Phase 1) | 5.5.1 | ✅ | MatchingService running |
| AI matching (embedding Phase 2) | 5.5.1 | ✅ | Python FastAPI + sentence-transformers |
| `GET /api/matches?jobId=` | 5.5.3 | ✅ | MatchController |
| Stripe payment intent creation | 5.6.2 | ✅ | `POST /api/v1/payments` |
| Stripe webhook handler (idempotent) | 5.6.2 | ✅ | WebhookController + WebhookEvent table |
| Escrow release | 5.6.4 | ✅ | `POST /api/v1/payments/{id}/release` |
| Refund | 5.6.4 | ✅ | `POST /api/v1/payments/{id}/refund` |
| **Get my payments** | 5.6.4 | ✅ | **BUG-3 fixed** — `GET /api/v1/payments/mine` |
| Messaging conversations + messages | 5.7.3 | ✅ | REST + STOMP WebSocket |
| Message read receipts | 5.7.3 | ✅ | `PATCH /api/messages/{id}/read` |
| Notifications (WebSocket + polling) | 5.8.5 | ✅ | WebSocket `/ws/notifications` |
| Mark notification read | 5.8.5 | ✅ | `PATCH /api/notifications/{id}/read` |
| **Mark all notifications read** | 5.8.5 | ❌ Missing | `PATCH /api/notifications/read-all` endpoint — not wired in UI |
| **Notification preferences** | 5.8.4–5.8.5 | ❌ Missing | NotificationPreference entity + UI not built |
| Full-text job search (Elasticsearch) | 5.9.2 | ✅ | SearchController |
| Full-text freelancer search (Elasticsearch) | 5.9.2 | ⚠️ Data gap | Endpoint works; needs indexed profiles |
| Search suggestions (autocomplete) | 5.9.2 | ✅ | `/api/search/suggestions` |
| Circuit breakers + resilience | 6.1 | ✅ | Resilience4j on all services |
| Prometheus metrics | 6.3.1 | ✅ | All services expose `/actuator/prometheus` |
| Distributed tracing (OpenTelemetry) | 6.3.2 | ✅ | Micrometer + Tempo |
| Outbox pattern | 4.3 | ✅ | All event-publishing services |
| Idempotent consumers | 4.4 | ✅ | Webhook + Kafka consumers |

### 3.2 Frontend Pages & Flows

| Page / Feature | SRS Section | Route | Status | Notes |
|---|---|---|---|---|
| Landing page | 8.3 | `/` | ✅ | Auth redirect to `/dashboard` |
| Registration (CLIENT/FREELANCER) | 5.2.2 | `/auth/register` | ✅ | Role picker + Keycloak |
| Login (credential + social) | 5.2.2 | `/auth/login` | ✅ | PKCE + Google/GitHub |
| **401 silent token refresh** | 8.4 | (interceptor) | ⚠️ Partial | Auth interceptor exists but 401 retry not implemented |
| **Loading spinner interceptor** | 8.4 | (interceptor) | ❌ Missing | LoadingInterceptor not implemented |
| Dashboard (role-specific) | 8.1 | `/dashboard` | ✅ | Hero, quick actions, job feed |
| Job list | 8.3.1 | `/jobs` | ✅ | Paginated, filters |
| Job detail + proposals | 8.3.1 | `/jobs/:id` | ✅ | Proposals, status transitions |
| Job create | 8.3.1 | `/jobs/create` | ✅ | BUG-4 fixed |
| **Accept proposal → fund escrow prompt** | 8.3.2 | `/jobs/:id` | ❌ Missing | On accept, should prompt to fund escrow |
| **Stripe.js payment UI** | 8.3.2 | `/jobs/:id` or `/payments` | ❌ Missing | Stripe Elements form not built |
| Payments list | 5.6.4 | `/payments` | ✅ | BUG-3 fixed; release + refund buttons |
| **Payment Stripe card form** | 8.3.2 | `/payments` or dialog | ❌ Missing | No Stripe.js integration in frontend |
| My profile (edit) | 5.3 | `/profile` | ✅ | BUG-1 fixed; skills, portfolio, reviews |
| **Submit a review** | 5.3.2 | `/jobs/:id` | ❌ Missing | ReviewController exists; no UI to post a review |
| Public profile | 5.3.2 | `/users/:id` | ✅ | Read-only; skills, rating |
| Search (jobs + freelancers tabs) | 5.9 | `/search` | ⚠️ Partial | Jobs tab OK; freelancers needs ES data (BUG-2 fix) |
| Messaging list | 5.7.3 | `/messages` | ✅ | Display names resolved via UserService |
| Conversation (WebSocket) | 5.7 | `/messages/:id` | ✅ | Typing indicator, infinite scroll, read receipts |
| Notifications list | 5.8.5 | `/notifications` | ✅ | Paginated |
| **Notification bell real-time** | 5.8.5 | (top bar) | ⚠️ Partial | Bell exists; WebSocket updates not wired in |
| **Notification preferences page** | 5.8.4 | `/notifications/preferences` | ❌ Missing | UI + route not built |
| Admin dashboard | 7.1 | `/admin` | ⚠️ Stub | Component exists; no real content |
| Role-based navigation | 8.1 | (nav) | ✅ | Admin/Freelancer/Client nav |

---

## 4. Remaining Work — Prioritized

### Priority 1 — Critical Flows Not Yet Working

#### A. Stripe.js Payment UI (SRS §8.3.2)
The entire client-side payment flow is missing from the frontend:

1. When a client accepts a proposal → show "Fund Escrow" dialog
2. Dialog calls `POST /api/v1/payments` → gets `clientSecret`
3. Load Stripe.js, render `PaymentElement`, collect card info
4. Confirm payment with Stripe → wait for WebSocket notification
5. On success: show "Escrow funded — freelancer notified"

**What exists**: `PaymentService.createPaymentIntent()` frontend method and all backend endpoints.
**What's missing**: The `PaymentElement` Angular component and the trigger from job detail.

**Effort**: ~1 day. Files: `job-detail` component + new `payment-dialog` component.

#### B. 401 Token Refresh in Auth Interceptor (SRS §8.4)
The auth interceptor currently attaches the JWT but does not retry on 401. Per the SRS, on 401 it should:
1. Attempt silent refresh using the refresh token
2. Retry the original request with the new token
3. If refresh fails, redirect to `/auth/login`

**Effort**: ~2 hours. File: `auth.interceptor.ts`.

#### C. Submit Review UI (SRS §5.3.2)
Backend `POST /api/users/{id}/reviews` is complete. Frontend needs:
- "Leave Review" button on completed job detail
- Rating + comment dialog
- Guard: only available when job is COMPLETED

**Effort**: ~3 hours.

---

### Priority 2 — Important Missing Features

#### D. Admin Dashboard Content (SRS §7.1)
Currently a stub. The SRS defines admin-only access for platform oversight. Needed:
- Platform stats: total users, active jobs, total transaction volume
- User list with role and status
- Job moderation: flag/close inappropriate posts

Requires new admin-only endpoints on user-service and payment-service (gated by `ROLE_ADMIN`).

**Effort**: ~1.5 days.

#### E. Notification Bell Real-Time Updates (SRS §5.8.5)
`NotificationBellComponent` shows a static badge. It needs to:
- Connect to `/ws/notifications` WebSocket on app init
- Live-increment badge when notification arrives
- Show dropdown with last 5 notifications
- Mark as read on click

**Effort**: ~4 hours.

#### F. Notification Preferences Page (SRS §5.8.4)
Backend entity + endpoints exist. Frontend needs:
- Route `/notifications/preferences`
- Toggle: email / push / real-time notifications
- Quiet hours time picker

**Effort**: ~3 hours.

---

### Priority 3 — Polish and Enhancements

#### G. Mark All Notifications Read (SRS §5.8.5)
Backend `PATCH /api/notifications/read-all` exists. Just need a "Mark all as read" button on the notifications page.

**Effort**: ~30 minutes.

#### H. Loading Spinner Interceptor (SRS §8.4)
`LoadingSpinnerComponent` exists. A `LoadingInterceptor` that shows it for requests taking > 500ms is missing.

**Effort**: ~1 hour.

#### I. Search UX Improvements (SRS §5.9.2)
- Freelancer search cards should show avatar, skills, hourly rate, avg rating
- Budget range sliders for job search
- Min-rating filter for freelancer tab

**Effort**: ~1 day.

#### J. Public Profile Enhancements (SRS §5.3.2)
- "Message" button → creates conversation and navigates to it
- "Invite to Job" → client can invite freelancer to a specific job
- Portfolio links as clickable cards

**Effort**: ~4 hours.

---

## 5. Technical Debt

| Item | Priority | Notes |
|---|---|---|
| Replace `CommonModule` in `SearchComponent` | Low | Use `NgClass`, `DecimalPipe` directly |
| Convert `SearchComponent` to signals | Low | `jobResults`, `freelancerResults` etc. |
| `JobCreateComponent.submitting` to signal | Low | Currently plain boolean |
| Elasticsearch re-index for existing freelancers | Medium | One-time script after BUG-2 fix deployed |
| Add `ROLE_ADMIN` to registration form | Medium | Currently only CLIENT/FREELANCER selectable |
| `UserRole.ADMIN` missing from DB enum migration | High | Flyway migration needed if schema validates enum |
| Contract entity in Job Service | Medium | SRS §5.4.2 defines Contract; may not be implemented |
| E2E tests with Playwright | Low | Not yet started |

---

## 6. Deployment Checklist

- [ ] Replace `STRIPE_SECRET_KEY` placeholder with real test key
- [ ] Replace `stripePublishableKey` placeholder in `environment.prod.ts`
- [ ] Replace `stripePublishableKey` placeholder in `environment.ts` for local dev
- [ ] Set strong passwords in `.env` (`KC_DB_PASSWORD`, `KC_ADMIN_PASSWORD`, `DB_PASSWORD`)
- [ ] Configure Keycloak `redirectUris` for production domain
- [ ] Enable HTTPS in Nginx config
- [ ] Set up Grafana alerting rules (circuit breaker, DLQ depth, payment failure rate)
- [ ] Add Flyway migration if `UserRole` enum needs `ADMIN` value in DB

---

## 7. Quick-Start (Local Development)

```bash
# 1. Start all infrastructure
cp .env.example .env   # fill in passwords
docker compose up -d

# 2. Start Angular dev server
cd frontend/unikly-app
npm install
ng serve --proxy-config proxy.conf.json

# 3. Pre-seeded test users
testclient@unikly.com     / Test1234!   → ROLE_CLIENT
testfreelancer@unikly.com / Test1234!   → ROLE_FREELANCER
admin@unikly.com          / Admin1234!  → ROLE_ADMIN
```

> **Note**: Test users are seeded in Keycloak but have no DB profile row. BUG-1 fix auto-creates profiles on first login. For Elasticsearch freelancer data, a newly registered freelancer via `/auth/register` will now be indexed (BUG-2 fix).
