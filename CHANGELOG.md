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
