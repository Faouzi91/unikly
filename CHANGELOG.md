# Changelog

All notable changes to Unikly will be documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [6.6.0] - 2026-03-22

### Step 6.6 - Admin Dashboard & Auth Stability

#### Added

- **Backend Admin REST API** — new cross-service endpoints secured with `@PreAuthorize("hasRole('ROLE_ADMIN')")`:
  - `GET /api/users/admin/stats` and `GET /api/users/admin/directory` in `user-service`
  - `GET /api/jobs/admin/stats` and `PATCH /api/jobs/admin/{id}/close` in `job-service`
  - `GET /api/v1/payments/admin/stats` evaluating JPQL total escrow volume in `payment-service`
- **Frontend `AdminService`** (`core/services/admin.service.ts`) — connects to backend utilizing `rxjs/forkJoin` for aggregate dashboard metrics visualization
- **Frontend `AdminDashboardComponent`** (`features/admin/`) — live UI rendering `totalUsers`, `totalActiveJobs`, `totalEscrowVolume`, and a paginated "User Directory" table displaying user roles with badges

#### Changed

- **Frontend `auth.interceptor.ts`** — converted the 401 retry interceptor to use a `BehaviorSubject<boolean>` to queue all outgoing requests when a token refresh is active, effectively mitigating "logout storms" during simultaneous requests

#### Fixed

- **Angular Compilation Failure** — fixed `TS2305` error for incorrect `HttpClient` import inside `AdminService` enabling successful `docker-compose` builds
- **Integration Stability** — manually verified correct routing, existing Stripe.js UI integration, and `NotificationPreferences` toggle behaviors


## [6.5.0] - 2026-03-21

### Step 6.5 - Tailwind Frontend Refactor + Keycloak First-Party Auth Flow

#### Added

- **`backend/user-service/src/main/java/com/unikly/userservice/api/AuthenticationController.java`** - public auth APIs:
  - `POST /api/users/login` (direct credentials sign-in)
  - `POST /api/users/refresh` (refresh token exchange)
  - `POST /api/users/social/authorization-url` (provider authorization URL generation)
  - `POST /api/users/social/exchange` (OAuth code exchange with PKCE verifier)
- **`backend/user-service/src/main/java/com/unikly/userservice/application/AuthenticationService.java`** - server-side token exchange with Keycloak (`password`, `refresh_token`, `authorization_code`) and provider normalization (`google`, `facebook`, `microsoft`)
- **`backend/user-service/src/main/java/com/unikly/userservice/config/KeycloakAuthProperties.java`** and request DTOs (`LoginRequest`, `RefreshTokenRequest`, `SocialAuthorizationRequest`, `SocialCodeExchangeRequest`) with input validation
- **`frontend/unikly-app/src/app/core/services/toast.service.ts`** and **`frontend/unikly-app/src/app/shared/components/toast-stack/*`** - lightweight in-app toast notifications replacing Angular Material snackbar dependency
- **`frontend/unikly-app/proxy.conf.json`** - local dev proxy for `/api` and `/ws` through the gateway

#### Changed

- **Frontend UX/UI (Tailwind-first redesign)** - major visual and interaction refactor across landing, auth, jobs, search, profile, messaging, notifications, payments, and layout components for a cleaner marketplace-style light theme
- **Auth screens** (`login.component.*`, `register.component.*`) - improved field validation feedback, clearer error states, and social sign-in/sign-up entry points (Google, Facebook, Microsoft)
- **`frontend/unikly-app/src/styles.scss`** - rebuilt design tokens, component utility classes, and animation primitives to standardize visual language without Angular Material
- **`frontend/unikly-app/package.json`** - removed Angular Material/CDK/animation dependencies; app now relies on custom Tailwind components
- **`frontend/unikly-app/src/app/core/auth/keycloak.service.ts`** - moved to backend-mediated auth requests, token persistence/refresh handling, and PKCE social flow state management
- **`frontend/unikly-app/src/app/core/auth/auth.interceptor.ts`** - excludes `/api/users/login`, `/api/users/refresh`, and `/api/users/social/**` from bearer token attachment
- **`frontend/unikly-app/src/app/core/auth/error.interceptor.ts`** - centralized toast-based API error surfacing by status tier
- **`frontend/unikly-app/src/environments/environment.ts`** and `npm start` script - switched API/WS URLs to relative paths (`/api`, `/ws`) for proxy and nginx compatibility
- **`infra/keycloak/unikly-realm.json`** - expanded frontend redirect/web origins and seeded test users for `ROLE_CLIENT`, `ROLE_FREELANCER`, and `ROLE_ADMIN`

#### Fixed

- **403 on sign-in requests** - gateway and user-service security configs now permit unauthenticated access to login, refresh, and social auth endpoints
- **Auth API error propagation** - global exception handling now maps `ResponseStatusException` to structured client-readable error payloads
- **Cross-origin/login compatibility** - gateway CORS now supports configurable allowed-origin patterns instead of a single hardcoded origin
- **Container health check reliability** - gateway and user-service runtime images include `curl` for health probe commands

---

## [6.4.0] - 2026-03-21

### Step 6.4 — Docker Build Optimization: Shared Java Builder Base Image

#### Added

- **`infra/docker/Dockerfile.java-builder`** — shared multi-stage build base for all 8 Java
  microservices; downloads the Gradle wrapper distribution once and bakes it into a single
  `localhost/unikly/java-builder:latest` image layer reused by every service build

#### Changed

- **All 8 Java service Dockerfiles** (`Dockerfile.gateway`, `Dockerfile.job-service`,
  `Dockerfile.user-service`, `Dockerfile.payment-service`, `Dockerfile.matching-service`,
  `Dockerfile.messaging-service`, `Dockerfile.notification-service`, `Dockerfile.search-service`) —
  removed duplicated Layers 1–2 (gradlew setup + wrapper download); now `FROM localhost/unikly/java-builder:latest AS build`
- **`build.sh`** — added `docker build … -t localhost/unikly/java-builder:latest` step before
  `docker compose build` so the base image is always present before services are built
- **`backend/build.gradle.kts`** — updated Gradle Java toolchain from `languageVersion=21` to
  `languageVersion=25` to match the `eclipse-temurin:25-jdk` Docker base image
- **`.dockerignore`** — added `backend/*/bin/` to exclude compiled class files from build context

#### Fixed

- **BuildKit registry resolution** — base image tagged with `localhost/` prefix so Docker does not
  attempt to pull it from Docker Hub registry during `docker compose build`
- **Gradle toolchain mismatch** — services were failing with
  `Cannot find a Java installation matching languageVersion=21` because the container only has JDK 25;
  aligning the toolchain declaration with the actual JDK eliminates the error

---

## [6.3.0] - 2026-03-19

### Step 6.3 — Integration Testing with Testcontainers

#### Added

- **`job-service/src/test/java/.../JobLifecycleIntegrationTest.java`** — 4 integration tests:
  - `testCreateJob_returnsCreated` — POST /api/v1/jobs → 201, status=DRAFT
  - `testCreateJob_invalidTitle_returns400` — blank title → 400 with field error detail
  - `testFullJobLifecycle` — DRAFT → OPEN → proposal submitted → contract accepted
  - `testInvalidStatusTransition_returns409` — DRAFT → COMPLETED raises 409 Conflict
- **`payment-service/src/test/java/.../PaymentWebhookIntegrationTest.java`** — 3 integration tests:
  - `testCreatePaymentIntent_success` — mocked Stripe SDK, POST /api/v1/payments → 201 PENDING
  - `testWebhookProcessing_updatesToFunded` — Stripe webhook → payment FUNDED, LedgerEntry created
  - `testWebhookIdempotency_processesOnce` — same webhook twice → exactly one LedgerEntry
- **`matching-service/src/test/java/.../MatchingEngineIntegrationTest.java`** — 2 integration tests:
  - `testRuleBasedMatching_producesScores` — 5 freelancers, AI mocked empty, rule-based scores in [0,1]
  - `testIdempotentConsumer_skipsDuplicate` — same JobCreated event twice → exactly 1 match result
- **Test `application-test.yml`** for job-service, payment-service, and matching-service
  (mocked JWT issuer URI, Flyway migration path, `ddl-auto: validate`, tracing disabled)

#### Fixed (Spring Boot 4.0 compatibility discovered during testing)

- **Flyway auto-configuration** — replaced `org.flywaydb:flyway-core` with
  `org.springframework.boot:spring-boot-starter-flyway` in all 6 services; Spring Boot 4.0 moved
  `FlywayAutoConfiguration` from `spring-boot-autoconfigure` to a separate `spring-boot-flyway` module
- **Kafka auto-configuration** — replaced `org.springframework.kafka:spring-kafka` with
  `org.springframework.boot:spring-boot-starter-kafka` in all 7 services; Spring Boot 4.0 moved
  `KafkaAutoConfiguration` to a separate `spring-boot-kafka` module
- **JPA entity/repository scanning** — added `@EntityScan` and `@EnableJpaRepositories` to
  job-service, payment-service, matching-service, and user-service `Application` classes to include
  `com.unikly.common` packages (`OutboxEvent` entity and `OutboxRepository` interface);
  `scanBasePackages` does not extend JPA auto-configuration scanning in Spring Boot 4.0
- **Jackson 2.x compatibility** — added `Jackson2CompatAutoConfiguration` to common module
  auto-configuration; Spring Boot 4.0 uses Jackson 3.x (`tools.jackson`) by default but services
  use `com.fasterxml.jackson.databind.ObjectMapper`
- **JSONB column binding** — added `@JdbcTypeCode(SqlTypes.JSON)` to `OutboxEvent.payload`;
  Hibernate 7.x (Spring Boot 4.0) requires explicit JSON type annotation for JSONB columns
- **Flyway migration `CHAR(3)` → `VARCHAR(3)`** — corrected `currency` column type in
  `payment-service/V1__create_payment_tables.sql`; Hibernate schema validation rejected `bpchar`
  when entity mapped `currency` as `String` (expected `varchar`)
- **`UnreachableFilterChainException`** — in `PaymentWebhookIntegrationTest`, renamed test
  `SecurityFilterChain` bean to `filterChain` with `allow-bean-definition-overriding: true`;
  Spring Security 7.x (Spring Boot 4.0) throws this exception when two chains both use `anyRequest()`

#### Updated

- **Testcontainers 2.0 artifact IDs** — updated all three services to use renamed modules:
  `testcontainers-postgresql`, `testcontainers-kafka`, `testcontainers-junit-jupiter`; used
  `ConfluentKafkaContainer("confluentinc/cp-kafka:7.6.0")` (full image reference required in TC 2.0)

---

## [6.2.0] - 2026-03-19

### Step 6.2 — Nginx & Production Docker Compose

#### Added

- **`infra/docker-compose.prod.yml`** — production override:
  - `restart: unless-stopped` on all services
  - Memory limits: gateway 512 MB, each backend service 768 MB, Kafka 1 GB,
    Elasticsearch 1 GB, each PostgreSQL 256 MB, matching-ai 1 GB, Redis 256 MB, nginx 128 MB
  - JSON-file logging (`max-size: 10m`, `max-file: 3`) on all services
  - `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` environment override on all Spring JPA services
  - `nginx` service (Dockerfile.frontend) exposed on port 80 as the sole external entry point
- **`Makefile`** — root-level developer shortcuts: `dev`, `dev-logs`, `dev-monitor`, `prod`,
  `build`, `down`, `clean`, `test`, `status`
- **`build.sh`** — end-to-end build script: Gradle `bootJar` → Angular production build →
  `docker compose build`

#### Updated

- **`infra/nginx/nginx.conf`** — rewritten as a full nginx config:
  - `worker_processes auto`, `worker_connections 1024`
  - Gzip compression for HTML, JS, CSS, JSON
  - Security headers: `X-Content-Type-Options`, `X-Frame-Options`, `X-XSS-Protection`,
    `Referrer-Policy`
  - Static asset caching (`Cache-Control: public, immutable`, `expires 1y`)
  - `/api/` proxy, `/ws/` WebSocket proxy, `/webhooks/` Stripe webhook proxy → gateway
- **`infra/docker/Dockerfile.frontend`** — updated nginx config COPY destination to
  `/etc/nginx/nginx.conf` (full-config path, matching the rewritten nginx.conf format)

---

## [6.1.0] - 2026-03-19

### Step 6.1 — AI Matching Microservice (Python/FastAPI)

#### Added

- **`matching-ai/`** — standalone Python FastAPI microservice for semantic freelancer–job matching:
  - `app/models.py` — Pydantic v2 models: `FreelancerProfile`, `MatchRequest`, `MatchScore`, `MatchResponse`
  - `app/matching_engine.py` — loads `all-MiniLM-L6-v2` on startup; computes cosine-similarity
    embeddings per freelancer; returns top-20 matches sorted by score (0.0–1.0)
  - `app/main.py` — FastAPI app with lifespan model loading, CORS middleware, request-logging
    middleware; endpoints `POST /api/ai/match` and `GET /health`
  - `app/config.py` — environment-driven settings (`MODEL_NAME`, `MAX_MATCHES`, `LOG_LEVEL`)
  - `Dockerfile` — Python 3.12-slim, pre-downloads model at build time, exposes port 8090
  - `README.md` — usage, Docker, and endpoint reference
- **`infra/docker-compose.yml`** — added `matching-ai` service (port 8090, 1 GB memory limit,
  60 s start period for model warm-up)

#### Updated

- **`backend/matching-service`** — hybrid AI + rule-based scoring:
  - `AiMatchingClient` — replaced stub with real `RestClient` call to `http://matching-ai:8090/api/ai/match`;
    pre-filters candidates by skill intersection; circuit breaker fallback returns empty list
  - `MatchingService` — injects `RuleBasedMatchingEngine`; runs both engines, merges with
    50 % AI + 50 % rule-based hybrid score (`MatchStrategy.AI_EMBEDDING`);
    falls back to 100 % rule-based when AI circuit is open; publishes actual strategy in `JobMatchedEvent`
  - `application.yml` — adds `matching.ai-service-url` property (env: `AI_SERVICE_URL`)

---

## [0.1.0] - 2026-03-17

### Step 0.1 — Project Initialization

#### Added
- Monorepo structure: `backend/`, `frontend/`, `matching-ai/`, `infra/`
- Root Gradle build (`build.gradle.kts`) with:
  - Spring Boot 4.0.3, Spring Cloud 2024.0.1
  - Java 25 toolchain (Gradle 9.4.0)
  - Lombok, Jackson, SLF4J as shared dependencies
  - Native `platform()` BOM imports (no dependency-management plugin)
- `common` module for shared DTOs and event classes
- `.gitignore`, `.env.example`, `README.md`

### Step 0.2 — Shared Event & DTO Library

#### Added
- **Sealed event interface** (`BaseEvent`) with Jackson polymorphic
  serialization (`@JsonTypeInfo` / `@JsonSubTypes`) — 11 event record types:
  `JobCreatedEvent`, `JobStatusChangedEvent`, `ProposalSubmittedEvent`,
  `ProposalAcceptedEvent`, `PaymentInitiatedEvent`, `PaymentCompletedEvent`,
  `EscrowReleasedEvent`, `JobMatchedEvent`, `MessageSentEvent`,
  `UserProfileUpdatedEvent`, `UserRegisteredEvent`
- **DTOs**: `Money`, `MatchEntry`, `PageResponse<T>` (Java records)
- **Outbox pattern**: `OutboxEvent` JPA entity + `OutboxPublisher` scheduled
  publisher (polls every 500ms, retries up to 5x, resolves Kafka topic from
  event type)
- **UserContext** security utility — extracts userId/roles from JWT or
  gateway-injected `X-User-Id` / `X-User-Roles` headers

---

## [1.0.0] - 2026-03-19

Initial release of Unikly — a fully event-driven microservices freelancing platform with AI matching, Stripe escrow payments, real-time messaging, and comprehensive observability.

### Step 1.1 — Docker Compose: Core Infrastructure

#### Added
- **Docker Compose** (`infra/docker-compose.yml`) with 12 services:
  - 7 PostgreSQL 18 instances (jobs :5433, users :5434, payments :5435,
    matching :5436, messaging :5437, notifications :5438, keycloak :5439)
  - Kafka 4.2.0 in KRaft mode (no ZooKeeper) on port 9092
  - Kafka topic initializer (job.events, payment.events, matching.events,
    messaging.events, user.events, notification.dlq, payment.dlq)
  - Keycloak 26.5.5 on port 8180
  - Elasticsearch 9.0 on port 9200
  - Redis 7.4 on port 6379
- **Monitoring stack** (`infra/docker-compose.monitoring.yml`):
  - Prometheus on port 9090 with scrape configs for all 8 services
  - Grafana on port 3000
- **Prometheus config** (`infra/prometheus/prometheus.yml`) — scrapes
  `/actuator/prometheus` on gateway through search-service
- **Environment file** (`infra/.env`) for local development secrets

### Step 1.2 — Keycloak Realm Configuration

#### Added
- **Keycloak realm** `unikly` (`infra/keycloak/unikly-realm.json`) with:
  - `unikly-frontend` client — public, PKCE (S256), redirects to
    `localhost:4200`
  - `unikly-backend` client — confidential, service account enabled
  - Realm roles: `ROLE_CLIENT`, `ROLE_FREELANCER`, `ROLE_ADMIN`
  - Registration enabled, 5-min access token, 30-day SSO session
- **Test user setup script** (`infra/keycloak/setup-test-users.sh`) — creates 3
  users via Keycloak Admin REST API:
  - `client1@test.com` → ROLE_CLIENT
  - `freelancer1@test.com` → ROLE_FREELANCER
  - `admin@test.com` → ROLE_ADMIN
- **Docker Compose** volume mount for realm auto-import on Keycloak startup

### Step 1.3 — API Gateway Service

#### Added
- **Spring Cloud Gateway** module (`backend/gateway/`) with route configuration
  for all 7 downstream services, webhooks passthrough, and WebSocket support
- **JWT validation** via Keycloak OAuth2 Resource Server integration
- **JwtHeaderRelayFilter** — global filter that extracts `sub` and
  `realm_access.roles` from JWT, relays as `X-User-Id` / `X-User-Roles` headers
  to downstream services (skips `/webhooks/**`)
- **Redis rate limiting** — 100 requests/min per authenticated user using
  reactive Redis key resolver
- **Resilience4j circuit breakers** — per-route circuit breaker with JSON
  fallback (503 Service Unavailable)
- **CORS configuration** — allows `http://localhost:4200` with credentials
- **Security config** — permits `/webhooks/**`, `/actuator/**`, and
  `/fallback/**`; all other routes require JWT
- **Dockerfile** (`infra/docker/Dockerfile.gateway`) — multi-stage build with
  Java 25
- **Docker Compose** gateway service on port 8080, depends on Keycloak and Redis

### Step 2.1 — User Profile Service

#### Added
- **User Profile Service** module (`backend/user-service/`) — full DDD package
  structure: `domain`, `application`, `infrastructure`, `api`
- **Domain entities**:
  - `UserProfile` — UUID PK (same as Keycloak userId), display name, bio, avatar,
    role (CLIENT/FREELANCER), skills (PostgreSQL TEXT[]), hourly rate, currency,
    location, portfolio links (TEXT[]), average rating, total reviews, optimistic
    locking (`@Version`)
  - `Review` — reviewer/reviewee/job UUIDs, rating (1–5), comment, timestamp
- **Application services**:
  - `UserProfileService` — create/update profile, get profile, search freelancers
    by skill, publishes `UserProfileUpdatedEvent` via outbox pattern
  - `ReviewService` — create review (self-review validation), auto-recalculates
    `averageRating` and `totalReviews` on reviewee profile
- **REST API** (`/api/users`):
  - `GET /me` — current user's profile
  - `PUT /me` — update current user's profile
  - `GET /{id}` — public profile
  - `GET ?role=FREELANCER&skill=Angular&page=0&size=20` — search freelancers
  - `POST /{id}/reviews` — submit a review
  - `GET /{id}/reviews?page=0&size=10` — list reviews
- **GlobalExceptionHandler** — 400 (validation), 404, 409 (optimistic lock), 500
  with traceId
- **MapStruct mappers** for entity ↔ DTO conversion
- **Flyway migration** `V1__create_user_tables.sql` — user_profiles, reviews,
  outbox_events tables with indexes
- **Dockerfile** (`infra/docker/Dockerfile.user-service`) — multi-stage Java 25
- **Docker Compose** user-service on port 8082, depends on postgres-users + kafka

### Step 2.2 — Job Service: Domain Model & State Machine

#### Added
- **Job Service** module (`backend/job-service/`) — full DDD package structure
- **Domain entities**:
  - `Job` — UUID PK, clientId, title, description, budget, currency, skills
    (PostgreSQL TEXT[] with GIN index), status with `@Version` optimistic locking
  - `Proposal` — jobId (indexed FK), freelancerId, proposedBudget, coverLetter,
    status (PENDING/ACCEPTED/REJECTED/WITHDRAWN), `@Version`
  - `Contract` — unique jobId FK, clientId, freelancerId, agreedBudget, terms,
    status (ACTIVE/COMPLETED/TERMINATED)
- **Job lifecycle state machine** (`JobStatusMachine`):
  DRAFT → OPEN → IN_PROGRESS → COMPLETED → CLOSED, with CANCELLED, DISPUTED,
  REFUNDED branches. Validates all transitions, throws
  `InvalidStatusTransitionException` on invalid moves
- **Application services**:
  - `JobService` — create job, list with dynamic filtering (Spring Data
    Specifications for status/skill/budget range), update (DRAFT only), status
    transitions with event publishing
  - `ProposalService` — submit proposal (validates OPEN + not own job), accept
    (rejects all other PENDING, creates Contract, transitions to IN_PROGRESS),
    reject, list (job owner only)
- **REST API** (`/api/jobs`):
  - `POST /` — create job (ROLE_CLIENT)
  - `GET /?status&skill&minBudget&maxBudget&sort&page&size` — filter/paginate
  - `GET /{id}` — job details + proposal count
  - `PATCH /{id}` — update (DRAFT only, owner)
  - `PATCH /{id}/status` — transition status (owner)
  - `POST /{id}/proposals` — submit proposal (ROLE_FREELANCER)
  - `GET /{id}/proposals` — list proposals (owner)
  - `PATCH /{id}/proposals/{pid}/accept` — accept + auto-create contract
  - `PATCH /{id}/proposals/{pid}/reject` — reject proposal
- **Events via outbox**: `JobCreatedEvent`, `JobStatusChangedEvent`,
  `ProposalSubmittedEvent`, `ProposalAcceptedEvent`
- **GlobalExceptionHandler** — 400, 403, 404, 409, 500 with traceId
- **MapStruct mappers**: `JobMapper`, `ProposalMapper`, `ContractMapper`
- **Flyway migration** `V1__create_job_tables.sql` — jobs, proposals, contracts,
  outbox_events with GIN index on skills
- **`UserContext.requireRole()`** added to common module
- **Dockerfile** + **Docker Compose** job-service on port 8081

### Step 2.3 — Job Service: Event Consumption

#### Added
- **PaymentEventConsumer** — `@KafkaListener` on `payment.events` topic
  (group `job-service-group`) handling:
  - `PaymentCompletedEvent` — auto-transitions job OPEN → IN_PROGRESS when an
    accepted proposal exists
  - `EscrowReleasedEvent` — logs settlement event for tracking
- **Idempotent consumer pattern**:
  - `ProcessedEvent` entity with `event_id` UUID primary key + `processed_at`
  - `ProcessedEventRepository` — checks for duplicate eventIds before processing
  - Deduplication and event processing in the same `@Transactional` block
  - Flyway migration `V2__create_processed_events.sql`
- **Dead Letter Queue** — `DefaultErrorHandler` with `FixedBackOff(1000ms, 3
  retries)` + `DeadLetterPublishingRecoverer` routing to `job-service.dlq` topic
- **Kafka consumer configuration** — `auto-offset-reset: earliest`,
  `enable-auto-commit: false`, `ack-mode: RECORD`, `concurrency: 3`
- **`job-service.dlq`** topic added to Docker Compose kafka-init
- **ARCHITECTURE.md** — system architecture diagram with service connections,
  event flows, Kafka topics, security flow, and implementation progress

### Step 2.4 — Search Service

#### Added
- **Search Service** module (`backend/search-service/`) — Elasticsearch-backed
  full-text search with Kafka event sync
- **Elasticsearch documents**:
  - `JobDocument` (`@Document(indexName = "jobs")`) — jobId, title (analyzed),
    description (analyzed), skills (keyword), budget, currency, status, clientId,
    createdAt
  - `FreelancerDocument` (`@Document(indexName = "freelancers")`) — userId,
    displayName (analyzed), skills (keyword), hourlyRate, averageRating,
    location, bio (analyzed)
  - `ProcessedEventDocument` (`@Document(indexName = "processed-events")`) —
    Elasticsearch-based idempotent consumer tracking
- **Kafka consumers** (idempotent, with DLQ):
  - `JobEventConsumer` — `job.events` group `search-service-group`:
    `JobCreatedEvent` → index new job; `JobStatusChangedEvent` → update status
    or delete from index if CANCELLED/CLOSED
  - `UserEventConsumer` — `user.events` group `search-service-group`:
    `UserProfileUpdatedEvent` → re-index freelancer via REST call to
    user-service with event data fallback
- **`UserProfileClient`** — REST client fetching full user profiles from
  user-service for Elasticsearch indexing
- **`SearchService`** — Elasticsearch `NativeQuery` with `BoolQuery`:
  - `searchJobs`: multi_match on title+description, filter by skills/budget
    range, always filter `status=OPEN`, sort by _score then createdAt desc
  - `searchFreelancers`: multi_match on displayName+bio+skills, filter by
    skills/minRating, sort by _score then averageRating desc
  - `getSuggestions`: prefix-matched skill suggestions from curated skill set
- **REST API** (`/api/v1/search`):
  - `GET /jobs?q=&skills=&minBudget=&maxBudget=&page=&size=`
  - `GET /freelancers?q=&skills=&minRating=&page=&size=`
  - `GET /suggestions?q=&type=skill`
- **DLQ** — `search-service.dlq` topic with `FixedBackOff(1000ms, 3 retries)`
- **Dockerfile** (`infra/docker/Dockerfile.search-service`) — multi-stage Java 25
- **Docker Compose** search-service on port 8087, depends on elasticsearch + kafka
- **ARCHITECTURE.md** updated with search-service flows and connections

### Step 2.5 — Angular Frontend: Project Setup & Auth

#### Added
- **Angular 21 frontend** (`frontend/unikly-app/`) scaffolded with SCSS, routing,
  standalone components
- **Dependencies**: Angular Material 21, Tailwind CSS 4, keycloak-js,
  @stomp/stompjs, ngx-toastr, ngx-spinner, sweetalert2
- **Keycloak authentication**:
  - `KeycloakService` — PKCE S256 init, login/logout, token management, role
    checks, silent SSO check
  - `authGuard` — CanActivateFn, redirects to Keycloak login if unauthenticated
  - `roleGuard` — CanActivateFn factory, checks required realm role
  - `authInterceptor` — attaches Bearer JWT to `/api/**` requests, handles 401
    with silent token refresh + retry
  - `errorInterceptor` — catches HTTP errors, shows MatSnackBar for 4xx/5xx
    (skips 401)
- **`ApiService`** — generic typed HTTP methods (get, post, put, patch, delete)
  with base URL from environment
- **Feature placeholders**: auth, jobs (list/create/detail), profile (my/public),
  search, messaging (list/conversation), notifications, payments
- **Shared components**: `LoadingSpinnerComponent` (Tailwind animated),
  `TimeAgoPipe` (relative date formatting)
- **Layouts**:
  - `MainLayoutComponent` — MatSidenav (Jobs, Search, Messages, Profile) +
    MatToolbar (notifications bell, user menu with logout)
  - `AuthLayoutComponent` — centered MatCard for auth flows
- **Routing** (`app.routes.ts`) — lazy-loaded routes with auth/role guards,
  default redirect to `/jobs`
- **App config** — `APP_INITIALIZER` for Keycloak, HTTP interceptors, animations
- **Environment config** — apiUrl, wsUrl, Keycloak URL/realm/clientId for dev
  and production
- **Nginx config** (`infra/nginx/nginx.conf`) — SPA routing with `try_files`,
  `/api/` proxy to gateway, `/ws/` WebSocket proxy with upgrade headers, gzip
- **Dockerfile** (`infra/docker/Dockerfile.frontend`) — multi-stage Node 22
  build + Nginx 1.27 Alpine runtime

### Step 2.6 — Angular: Job Features

#### Added
- **Job models** (`features/jobs/models/job.models.ts`) — TypeScript interfaces:
  Job, Proposal, Contract, MatchEntry, PageResponse, CreateJobRequest,
  SubmitProposalRequest, JobSearchResult, FreelancerSearchResult
- **JobService** (`features/jobs/services/job.service.ts`) — API methods:
  getJobs (search), getMyJobs, getJob, createJob, updateJob, updateJobStatus,
  submitProposal, getProposals, acceptProposal, rejectProposal, getMatches,
  getSuggestions
- **Job list page** (`job-list.component.ts`) — responsive Material card grid
  (3/2/1 cols), collapsible filter panel with search (300ms debounce), skill
  autocomplete via search suggestions API, min/max budget inputs, MatPaginator,
  empty state, status badges with color coding
- **Job create page** (`job-create.component.ts`) — reactive form with
  validation (title max 200, description required, budget > 0, currency
  dropdown USD/EUR/XAF/GBP), MatChipInput with skill autocomplete, submit
  navigates to job detail with success snackbar
- **Job detail page** (`job-detail.component.ts`) — header with title, status
  badge, budget, proposal count, skills chips; tab group:
  - Tab 1 "Details" — full description
  - Tab 2 "Proposals" (job owner only) — proposal list with freelancer link,
    proposed budget, cover letter, accept/reject buttons with SweetAlert2
    confirmation dialogs
  - Tab 3 "Matches" (job owner only) — matched freelancers with match score
    percentage, skills, hourly rate, rating
- **Proposal dialog** (`components/proposal-dialog.component.ts`) — MatDialog
  with proposed budget input and cover letter textarea (min 50 chars), shown
  via FAB button for freelancers on OPEN jobs

### Step 2.7 — Angular: Profile & Search Pages

#### Added
- **Reusable shared components**:
  - `StarRatingComponent` — 5-star display (filled/half/empty), interactive mode
    with hover + click, optional numeric value display
  - `SkillChipsComponent` — MatChip list with optional editable mode and remove
    events
  - `UserAvatarComponent` — image avatar or colored initial circle fallback,
    configurable size, deterministic color from name hash
- **User models** (`features/profile/models/user.models.ts`) — TypeScript
  interfaces: UserProfile, UserProfileRequest, Review, PageResponse
- **UserService** (`features/profile/services/user.service.ts`) — API methods:
  getMyProfile, updateMyProfile, getProfile, getReviews
- **Profile page** (`profile.component.ts`) — view/edit mode toggle:
  - View mode: avatar, display name, role, location, bio, skills chips, hourly
    rate, portfolio links, average rating with stars, total reviews, member since
  - Edit mode: reactive form with displayName, bio, hourlyRate, currency,
    location, skill chip input with autocomplete, dynamic FormArray for
    portfolio link URLs (add/remove), save with success snackbar
  - Reviews tab: paginated review list with star ratings
- **Public profile page** (`public-profile.component.ts`) — read-only profile
  display with avatar, skills, hourly rate, clickable portfolio links, "Send
  Message" button (disabled placeholder for Phase 4), paginated reviews section
- **Search page** (`search.component.ts`) — unified search with MatTabGroup:
  - Shared search input with 300ms debounce + skill filter chips
    (add/remove/clear via autocomplete)
  - Jobs tab: condensed card results with title, description, skills, budget,
    posted time; click navigates to /jobs/{id}; paginated
  - Freelancers tab: user cards with avatar, name, location, star rating,
    skills, hourly rate; click navigates to /users/{id}; paginated
- **JobService** updated with `searchFreelancers` method for freelancer search
  API

### Step 3.1 — Matching Service (Rule-Based)

#### Added
- **Matching Service** module (`backend/matching-service/`) — rule-based freelancer
  matching engine with async Kafka-driven processing
- **Domain entities**:
  - `MatchResult` — UUID PK, jobId (indexed), freelancerId (indexed), score
    `NUMERIC(5,4)`, matchedSkills (`TEXT[]`), strategy (`RULE_BASED`/`AI_EMBEDDING`),
    createdAt
  - `FreelancerSkillCache` — local read-model: userId PK, skills (`TEXT[]` GIN
    indexed), hourlyRate, averageRating, updatedAt
  - `ProcessedEvent` — idempotent consumer deduplication
- **`MatchingEngine`** interface + **`RuleBasedMatchingEngine`** implementation:
  - Filters freelancers with at least 1 skill overlap
  - Scoring formula: skillOverlap × 0.40 + budgetCompat × 0.20 + ratingScore × 0.25
    + responseScore × 0.15 (placeholder 0.5), clamped to [0.0, 1.0]
  - Returns top 20 matches sorted by score DESC
- **`MatchingService`**:
  - `processJobForMatching` — runs engine, saves results, publishes `JobMatchedEvent`
    via outbox pattern
  - `getMatchesForJob` / `getMatchesForFreelancer` — paginated retrieval
- **Kafka consumers** (both idempotent, DLQ with 3 retries):
  - `job.events` (`matching-service-group`) — on `JobCreatedEvent`, schedules matching
    with 30-second delay via `ScheduledExecutorService`
  - `user.events` (`matching-service-group`) — on `UserProfileUpdatedEvent`, upserts
    `FreelancerSkillCache`
- **Kafka producer**: `JobMatchedEvent` published to `matching.events` via outbox
- **REST API** (`/api/v1/matches`):
  - `GET /api/v1/matches?jobId={uuid}&limit=20`
  - `GET /api/v1/matches?freelancerId={uuid}&limit=10`
  - `GET /api/v1/matches/{id}`
- **Resilience4j** circuit breaker pre-configured for future AI matching service
  (`ai-matching`: 50% failure threshold, 30s open state, 10-event window)
- **`GlobalExceptionHandler`** — 400, 404, 500 with traceId
- **Flyway migration** `V1__create_matching_tables.sql` — freelancer_skill_cache
  (GIN index on skills), match_results (indexes on job_id, freelancer_id, score),
  processed_events, outbox_events
- **Dockerfile** (`infra/docker/Dockerfile.matching-service`) — multi-stage Java 25
- **Docker Compose** matching-service on port 8084, depends on postgres-matching + kafka
- **`settings.gradle.kts`** updated to include `matching-service`

### Step 3.2 — Notification Service

#### Added
- **Notification Service** module (`backend/notification-service/`) — hybrid real-time +
  push notification delivery with WebSocket STOMP, Redis presence, and Kafka consumers
- **Domain entities**:
  - `Notification` — UUID PK, userId (indexed), type (`NotificationType`), title
    (max 200), body (TEXT), read flag (partial index on unread), actionUrl, createdAt
  - `NotificationPreference` — userId PK, emailEnabled, pushEnabled,
    realtimeEnabled, quietHoursStart/End (nullable `LocalTime`)
  - `JobClientCache` — local read-model: jobId PK, clientId, title, freelancerId
    (populated from Kafka events to avoid cross-service coupling)
  - `ProcessedEvent` — idempotent consumer deduplication
- **`NotificationDeliveryService`**:
  - `createAndDeliver` — saves notification, loads preferences, sends via WebSocket
    if user is online, otherwise logs FCM push placeholder
  - `getNotifications` — paginated list with optional `unreadOnly` filter
  - `markRead` / `markAllRead` — ownership-validated mark-as-read
  - `getPreferences` / `savePreferences` — preference CRUD
- **`WebSocketPresenceManager`** — Redis TTL-based user presence (`user:{id}:online`,
  120s), `@Scheduled` heartbeat refreshes TTL every 60s for active sessions
- **`WebSocketAuthInterceptor`** — `ChannelInterceptor` on STOMP CONNECT: extracts
  JWT from `Authorization` / `token` header, validates with Spring JwtDecoder, sets
  user principal, registers presence in Redis
- **`WebSocketEventListener`** — handles `SessionDisconnectEvent` to clean up Redis
- **WebSocket** — STOMP endpoint `/ws/notifications` (SockJS), simple broker on
  `/user`, app prefix `/app`, user destination prefix `/user`
- **Kafka consumers** (all idempotent with DLQ, 3 retries):
  - `job.events` (`notification-service-group`): `JobCreatedEvent` → populate
    `JobClientCache`; `ProposalSubmittedEvent` → notify client; `ProposalAcceptedEvent`
    → update freelancerId cache + notify freelancer
  - `payment.events`: `PaymentCompletedEvent` → notify freelancer (looked up from
    cache); `EscrowReleasedEvent` → notify freelancer with amount
  - `matching.events`: `JobMatchedEvent` → notify client with match count
  - `messaging.events`: `MessageSentEvent` → notify recipient unless active in
    conversation (checked via Redis key `user:{id}:active-conversation`)
- **REST API** (`/api/v1/notifications`):
  - `GET /api/v1/notifications?unread=false&page=0&size=20`
  - `PATCH /api/v1/notifications/{id}/read`
  - `PATCH /api/v1/notifications/read-all`
  - `GET /api/v1/notifications/preferences`
  - `PUT /api/v1/notifications/preferences`
  - `WS /ws/notifications`
- **Flyway migration** `V1__create_notification_tables.sql` — notifications
  (partial index on unread), notification_preferences, job_client_cache,
  processed_events
- **Dockerfile** (`infra/docker/Dockerfile.notification-service`) — multi-stage Java 25
- **Docker Compose** notification-service on port 8086, depends on
  postgres-notifications + kafka + redis
- **`settings.gradle.kts`** updated to include `notification-service`

### Step 3.3 — Angular: Notification Bell & Real-Time

#### Added
- **`WebSocketService`** (`core/services/websocket.service.ts`) — `@stomp/stompjs` client
  connecting to `ws://localhost:8080/ws/notifications/websocket` with JWT `Authorization`
  header; auto-reconnect (2s delay); on disconnect starts 30s polling fallback via
  `GET /api/v1/notifications?unread=true`; exposes `notifications$: Subject<NotificationPayload>`
- **`NotificationService`** (`core/services/notification.service.ts`) — signal-based state:
  `unreadCount = signal<number>(0)`, `notifications = signal<NotificationItem[]>([])`; `init()`
  loads initial unread page and subscribes to `WebSocketService.notifications$`; methods:
  `getNotifications`, `markAsRead` (tap decrements count), `markAllRead` (tap resets to 0),
  `getPreferences`, `updatePreferences`
- **`NotificationBellComponent`** (`shared/components/notification-bell/`) — standalone
  `MatIconButton` with `MatBadge` showing live unread count (hidden when 0); opens `MatMenu`
  overlay with: header + "Mark all read" button, last-10 notifications with type icon / title /
  body / time-ago, unread dot indicator; click → `markAsRead` + navigate to `actionUrl`; footer
  "View all notifications" → `/notifications`; CSS bell-pulse animation on new notification
- **`NotificationListComponent`** (`features/notifications/`) — full-page `/notifications` view:
  `MatButtonToggle` All/Unread filter, paginated list (`MatPaginator`), per-item type icon +
  title + body + time-ago + unread indicator, click marks as read and navigates; inline
  preferences section with `MatSlideToggle` for email / push / real-time (saved on toggle change)
- **`main-layout.component.ts`** updated — replaced static bell button with
  `<app-notification-bell>` in the toolbar

### Step 4.1 — Payment Service: Stripe & Escrow

#### Added
- **Payment Service** module (`backend/payment-service/`) — financial escrow service with
  Stripe integration, state machine, and idempotent webhook processing
- **Domain entities**:
  - `PaymentStatus` enum — `PENDING, FUNDED, RELEASED, COMPLETED, FAILED, REFUNDED, DISPUTED`
    with enforced valid transitions via `canTransitionTo()` guard
  - `Payment` — UUID PK, jobId (indexed), clientId, freelancerId, `NUMERIC(12,2)` amount,
    currency `CHAR(3)`, `@Version` optimistic lock, `stripePaymentIntentId` (unique),
    `idempotencyKey` (unique), createdAt/updatedAt via `@PrePersist`/`@PreUpdate`;
    `transitionTo()` throws `IllegalStateException` on invalid state change
  - `LedgerEntry` — immutable financial record: paymentId FK, `LedgerEntryType`
    (`ESCROW_FUND, ESCROW_RELEASE, REFUND, FEE_DEDUCTION`), amount, currency, description
  - `WebhookEvent` — String PK = Stripe event ID (e.g. `evt_1234…`); prevents duplicate
    Stripe webhook processing
- **`PaymentService`** (all writes `@Transactional`):
  - `createPaymentIntent` — checks idempotencyKey uniqueness (409 if dup), calls Stripe
    `PaymentIntent.create`, saves `Payment(PENDING)`, publishes `PaymentInitiatedEvent` via outbox
  - `handleWebhook` — `Webhook.constructEvent` HMAC verification, skips if already in
    `webhook_events`, handles `payment_intent.succeeded` (→ FUNDED + `ESCROW_FUND` ledger +
    `PaymentCompletedEvent`) and `payment_intent.payment_failed` (→ FAILED)
  - `releaseEscrow` — ownership check, FUNDED guard, → RELEASED → COMPLETED,
    `ESCROW_RELEASE` ledger, `EscrowReleasedEvent` via outbox
    *(TODO: real Stripe Transfer once freelancer accounts onboarded)*
  - `requestRefund` — FUNDED guard, `Refund.create`, → REFUNDED, `REFUND` ledger entry
- **`StripeClient`** — wraps all Stripe SDK calls; each method decorated with
  `@CircuitBreaker(name="stripe")` + `@Retry(name="stripe")`;
  `constructWebhookEvent` is local HMAC-only (no Resilience4j); fallback throws
  `PaymentProviderUnavailableException`
- **Resilience4j config** — circuit breaker: 50% failure threshold, 30s open wait, window 10;
  retry: 3 attempts, 1s base, ×2 exponential; time limiter: 10s timeout
- **`SecurityConfig`** — `/webhooks/stripe` is `permitAll()` (JWT-free);
  all other endpoints require JWT; stateless session
- **REST API** (`/api/v1/payments`):
  - `POST /api/v1/payments` — create PaymentIntent, returns `{ paymentId, clientSecret }`
  - `GET  /api/v1/payments?jobId={uuid}` — list payments for a job
  - `POST /api/v1/payments/{id}/release` — release escrow (client only)
  - `POST /api/v1/payments/{id}/refund` — request refund (FUNDED only)
  - `POST /webhooks/stripe` — Stripe webhook (no JWT, signature-only auth)
- **`GlobalExceptionHandler`** — RFC 9457 `ProblemDetail` for 404, 409, 422, 403, 503
- **Flyway migration** `V1__create_payment_tables.sql` — payments (unique idempotency_key,
  indexed job_id/status/client_id), ledger_entries, webhook_events, outbox_events
- **Dockerfile** (`infra/docker/Dockerfile.payment-service`) — multi-stage Java 25
- **Docker Compose** — payment-service on port 8083, depends on postgres-payments + kafka;
  `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET` env vars
- **`settings.gradle.kts`** updated to include `payment-service`

### Step 4.2 — Angular: Payment UI with Stripe.js

#### Added
- **`@stripe/stripe-js`** added to `package.json`; `stripePublishableKey` placeholder added to `environment.ts` and `environment.prod.ts`
- **`PaymentService`** (`core/services/payment.service.ts`) — lazy singleton `loadStripe()`; `createPaymentIntent`, `getPaymentStatus`, `releaseEscrow`, `requestRefund`
- **`PaymentDialogComponent`** (`features/payments/payment-dialog.component.ts`) — `MatDialog` with summary step → Stripe `PaymentElement` mount → `confirmPayment({ redirect: 'if_required' })`; inline Stripe error display
- **`PaymentStatusComponent`** (`features/payments/payment-status.component.ts`) — color-coded status badge; Release / Refund actions shown to client when status is `FUNDED` (SweetAlert2 confirm)
- **`job-detail`** updated — "Fund Escrow" banner after proposal accepted; `<app-payment-status>` rendered once payment record exists

### Step 4.3 — Messaging Service

#### Added

- **`messaging-service`** (`backend/messaging-service/`) — Spring Boot 4.0.3 Gradle submodule on port 8085
- **`Conversation`** entity — UUID PK, `participantIds` (PostgreSQL `UUID[]`), optional `jobId`, `lastMessageAt` updated on each message
- **`Message`** entity — UUID PK, `conversationId` (FK indexed), `senderId`, `content` (TEXT), `MessageContentType` enum (TEXT / FILE_LINK / SYSTEM), nullable `readAt`
- **`ConversationService`** — `getOrCreateConversation` (find by participant set or create), `getUserConversations` (paginated, sorted by `lastMessageAt DESC`), `getById` (participant guard → 403)
- **`MessageService`** — participant validation, message persistence, `lastMessageAt` update, WebSocket push to online recipients via Redis presence check, `MessageSentEvent` via outbox (100-char preview)
- **WebSocket** (`/ws/messages` with SockJS, STOMP) — JWT auth via `ChannelInterceptor`, `/user/{recipientId}/queue/messages` delivery, `/app/typing` → `/user/{recipientId}/queue/typing` ephemeral indicator
- **`MessagingPresenceManager`** — Redis `msg-user:{userId}:online` key, TTL 120 s, refreshed every 60 s via scheduler
- **Flyway** `V1__create_messaging_tables.sql` — `conversations`, `messages`, `outbox_events` with GIN index on `participant_ids[]`
- **`Dockerfile.messaging-service`** (`infra/docker/`) — multi-stage eclipse-temurin:25
- **`docker-compose.yml`** updated — `messaging-service` on port 8085 with Postgres, Kafka, and Redis deps
- **`settings.gradle.kts`** updated — added `include("messaging-service")`

### Step 4.4 — Angular: Chat Interface

#### Added

- **`MessageWebSocketService`** (`core/services/message-websocket.service.ts`) — STOMP client connecting to `/ws/messages`; exposes `messages$` and `typing$` subjects; `sendTypingIndicator(conversationId, recipientId)` publishes to `/app/typing`
- **`MessagingService`** (`features/messaging/services/messaging.service.ts`) — `getConversations`, `getMessages`, `sendMessage`, `markAsRead`, `getOrCreateConversation` via REST API
- **`ConversationListComponent`** (`features/messaging/conversation-list.component.ts`) — `/messages` route; lists conversations sorted by `lastMessageAt DESC`; real-time reordering on incoming WS message; empty state
- **`ConversationComponent`** (`features/messaging/conversation.component.ts`) — `/messages/:id` route; chat bubble layout (sent right/indigo, received left/gray); real-time WS delivery; typing indicator animation; send-on-Enter / Shift+Enter newline; infinite scroll up for older messages via `IntersectionObserver`; auto-read on open; read receipt `done_all` icon
- **`public-profile.component`** updated — "Send Message" button calls `getOrCreateConversation` then navigates to `/messages/:id`
- **`app.routes.ts`** updated — `/messages` now loads `ConversationListComponent`

### Step 5.1 — Prometheus Metrics & Grafana Dashboards

#### Added

- **Management config** updated across all 8 services — `metrics` added to `management.endpoints.web.exposure.include`; `management.metrics.tags.service: ${spring.application.name}` added for per-service label in Prometheus
- **Job Service custom metrics** — `unikly_job_created_total` (Counter), `unikly_proposal_submitted_total` (Counter), `unikly_job_status_transition_seconds` (Timer)
- **Payment Service custom metrics** — `unikly_payment_completed_total` (Counter, tag: currency), `unikly_payment_failed_total` (Counter), `unikly_stripe_api_call_seconds` (Timer)
- **Matching Service custom metrics** — `unikly_match_generated_total` (Counter, tag: strategy), `unikly_matching_duration_seconds` (Timer), `unikly_matching_queue_size` (Gauge)
- **Grafana dashboards** (`infra/grafana/dashboards/`) — `service-overview.json` (request rate, error rate, p50/p95/p99, JVM heap, threads), `kafka-overview.json` (consumer lag, records in/out, DLQ depth), `business-metrics.json` (jobs, proposals, payments, matches over time), `alerts.json` (circuit breaker state, failure rate, DLQ depth, retry calls)
- **Grafana provisioning** (`infra/grafana/provisioning/`) — `datasources.yml` (Prometheus at `http://prometheus:9090`), `dashboards.yml` (auto-load from `/var/lib/grafana/dashboards`)
- **`docker-compose.monitoring.yml`** updated — Grafana volume mounts for provisioning and dashboards dirs; persistent `unikly-grafana-data` volume

### Step 5.2 — Distributed Tracing

#### Added

- **OTel tracing deps** added to all 8 `build.gradle.kts` files — `io.micrometer:micrometer-tracing-bridge-otel`, `io.opentelemetry:opentelemetry-exporter-otlp`, `net.logstash.logback:logstash-logback-encoder:8.0`
- **Tracing config** added to all 8 `application.yml` files — `management.tracing.sampling.probability: 1.0`, `management.otlp.tracing.endpoint: http://tempo:4318/v1/traces`
- **Kafka observation** enabled per service — `spring.kafka.listener.observation-enabled: true` (consumers) and `spring.kafka.template.observation-enabled: true` (producers) so traceId propagates through Kafka message headers
- **`MdcUserIdFilter`** (`common/observability/`) — `jakarta.servlet.Filter` that reads userId from JWT SecurityContext (or `X-User-Id` header) and puts it into SLF4J MDC; cleared after each request
- **`TracingAutoConfiguration`** (`common/observability/`) — `@AutoConfiguration` + `@ConditionalOnWebApplication(SERVLET)` registers `MdcUserIdFilter` for all 7 servlet services; skipped automatically by the reactive gateway
- **`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`** (`common/src/main/resources/`) — registers `TracingAutoConfiguration` for Spring Boot auto-configuration discovery
- **`logback-spring.xml`** created for all 8 services — `LogstashEncoder` outputs structured JSON; reads `spring.application.name` via `<springProperty>`; all MDC fields (traceId, spanId, userId) included automatically
- **`MdcLoggingFilter`** (`gateway/filter/`) — reactive `GlobalFilter` (order 1) that extracts `X-User-Id` from the relayed request header and applies it to MDC for gateway log lines
- **`infra/tempo/tempo.yaml`** — Grafana Tempo config; OTLP HTTP receiver on `0.0.0.0:4318`; local trace storage under `/var/tempo`; 48-hour block retention
- **`docker-compose.monitoring.yml`** updated — `unikly-tempo` service (ports 4318 OTLP HTTP, 3200 query UI); Grafana `depends_on` tempo; `unikly-tempo-data` volume
- **`infra/grafana/provisioning/datasources.yml`** updated — Tempo datasource added (`http://tempo:3200`); service map linked to Prometheus; node graph enabled

### Step 5.3 — Resilience Patterns Audit

#### Added

- **`AiMatchingClient`** (`matching-service/application/`) — `@CircuitBreaker(name="ai-matching")` placeholder for Step 6.1 AI service; always falls back to `RuleBasedMatchingEngine` until `matching-ai:8090` is deployed; logs "AI unavailable, using rule-based"
- **`MatchingService`** updated — injects `AiMatchingClient` instead of `MatchingEngine` directly so all matching calls are CB-protected
- **`SearchUnavailableException`** + **`GlobalExceptionHandler`** (`search-service/api/`) — Elasticsearch failures map to 503 `"Search temporarily unavailable"`
- **`SearchService`** updated — `searchJobs()` and `searchFreelancers()` wrapped in try-catch; throws `SearchUnavailableException` on ES error

#### Changed

- **`StripeClient`** (`payment-service`) — `createPaymentIntent` sets `readTimeout=5000ms` and `connectTimeout=5000ms` on Stripe SDK `RequestOptions`
- **`payment-service/application.yml`** — actuator exposure extended with `circuitbreakers,retries`
- **`matching-service/application.yml`** — actuator exposure extended with `circuitbreakers,retries`
- **`search-service/application.yml`** — actuator exposure extended with `circuitbreakers`

#### Verified (already in place)

- **Gateway** — all 7 routes have `CircuitBreaker` filter; `FallbackController` returns `503 { error, service }` ✅
- **StripeClient** — `@CircuitBreaker(name="stripe")` + `@Retry(name="stripe")` on all Stripe calls; `PaymentProviderUnavailableException` → 503 ✅
- **Kafka DLQ routing** — all 6 consumer services have `KafkaConfig` with `DeadLetterPublishingRecoverer` and 3 retries at 1s backoff ✅
- **Idempotent consumers** — all consumers check `processedEventRepository.existsById()` before processing ✅
- **Unique consumer group IDs** — each service has distinct `groupId` ✅

### Step 5.4 — OpenAPI Documentation

#### Added

- **`springdoc-openapi-starter-webmvc-ui:2.8.0`** dependency added to all 7 backend services (job, user, payment, matching, messaging, notification, search)
- **`springdoc-openapi-starter-webflux-api:2.8.0`** dependency added to gateway
- **`OpenApiConfig`** (`@OpenAPIDefinition` + JWT `@SecurityScheme`) created in each of the 7 services with service-specific title and description
- **`@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`** annotations added to all controllers: `JobController`, `ProposalController`, `UserProfileController`, `ReviewController`, `PaymentController`, `MatchController`, `ConversationController`, `NotificationController`, `SearchController`
- **`@Schema`** annotations added to key DTOs: `CreateJobRequest`, `JobResponse`, `SubmitProposalRequest`, `UserProfileRequest`, `CreatePaymentRequest`, `JobSearchResult`, `FreelancerSearchResult`

#### Changed

- **`springdoc.api-docs.path`** per service configured to match the gateway's routing prefix (e.g., `/api/jobs/api-docs`, `/api/users/api-docs`) enabling transparent proxy via existing gateway predicates
- **Gateway `application.yml`** — added `springdoc.swagger-ui.urls` block listing all 7 service api-docs endpoints; users visit `http://localhost:8080/swagger-ui.html` and select any service from the dropdown

### Step 5.5 — Error Handling & Validation Hardening

#### Added

- **`ErrorDetail`** record (`common/error/`) — field + message pair for validation errors
- **`ErrorResponse`** record (`common/error/`) — unified error body: `{ status, error, message, traceId, timestamp, details }` with static factory methods; `traceId` sourced from Micrometer MDC
- **`GlobalExceptionHandlerBase`** (`common/error/`) — abstract base class handling: `MethodArgumentNotValidException` → 400 with per-field `details`, `ConstraintViolationException` → 400, `EntityNotFoundException` → 404, `AccessDeniedException` → 403, `ObjectOptimisticLockingFailureException` → 409, `IllegalArgumentException` → 400, generic `Exception` → 500 (never exposes stack trace)
- **`RequestLoggingFilter`** (`common/observability/`) — logs `METHOD PATH user=X status=Y duration=Zms` at INFO level for every request
- **`GlobalExceptionHandler`** (`messaging-service/api/`) — new, extends base class
- **`GlobalExceptionHandler`** (`messaging-service`) — all 7 services' handlers now extend `GlobalExceptionHandlerBase` for consistent error format

#### Changed

- **`TracingAutoConfiguration`** — registers `RequestLoggingFilter` via `FilterRegistrationBean` (order +20, after MDC filter)
- **`GlobalExceptionHandler`** in job-service, user-service, matching-service, notification-service — rewritten to extend base class, service-specific exceptions preserved
- **`GlobalExceptionHandler`** in payment-service — migrated from `ProblemDetail` to `ErrorResponse`; preserves all payment-specific exception handlers
- **`GlobalExceptionHandler`** in search-service — migrated from `ProblemDetail` to `ErrorResponse`
- **`UserProfileRequest`** — added `@Size(max=5000)` validation on `bio` field
- **Angular `error.interceptor.ts`** — updated to parse structured `ErrorResponse`: 400 shows first field detail, 403 → "You don't have permission", 404 → "Resource not found", 409 → "This resource was modified", 503 → "Service temporarily unavailable", 500 → "Something went wrong" with traceId reference
