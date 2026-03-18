# Changelog

All notable changes to Unikly will be documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

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

## [Unreleased]

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
