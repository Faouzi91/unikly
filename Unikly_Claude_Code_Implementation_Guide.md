# 🚀 Unikly — Claude Code Implementation Guide

## AI-Powered Freelancing Platform | Step-by-Step Build Prompts

> **Version:** 1.0 — March 2026
> **Architecture:** Event-Driven Microservices with Domain-Driven Design (DDD)
> **From:** Empty directory → Production deployment
> **Total Steps:** 29 prompts across 6 phases

---

## 📋 How to Use This Document

This document contains **29 sequential prompts** organized into 6 phases. Each prompt is designed to be **copied and pasted directly into Claude Code**.

### Rules

1. **One prompt per Claude Code session.** Paste the prompt, let Claude Code execute it fully, verify the output, then move to the next.
2. **Every prompt ends with a Git commit.** The commit message is specified — do not change it.
3. **Every prompt updates `CHANGELOG.md`.** This creates a living record of every change.
4. **If a step fails, do not skip it.** Debug with Claude Code before proceeding.
5. **The SRS document is the source of truth.** If this guide and the SRS conflict, the SRS wins.

### Prerequisites

Before starting, install:

- Git
- Docker Desktop (with Docker Compose v2)
- Java 21 (OpenJDK)
- Node.js 22 LTS
- Gradle 8.14+
- Angular CLI 21.x (`npm install -g @angular/cli`)
- A Stripe test account with API keys

### Legend

Throughout this document:

| Icon | Meaning |
|------|---------|
| 📋 **PROMPT** | Copy-paste this into Claude Code |
| ✅ **VERIFY** | How to confirm the step worked |
| 📦 **COMMIT** | Exact git commit to run |
| 📝 **CHANGELOG** | Entry to add to CHANGELOG.md |
| ⚠️ **IMPORTANT** | Critical warnings — do not ignore |

---

## Table of Contents

- [Phase 0: Project Initialization](#phase-0-project-initialization)
  - [Step 0.1 — Create Monorepo & Initialize Git](#step-01--create-monorepo--initialize-git)
  - [Step 0.2 — Common Module: Shared Events, DTOs, Outbox Base](#step-02--common-module-shared-events-dtos-outbox-base)
- [Phase 1: Infrastructure Setup](#phase-1-infrastructure-setup)
  - [Step 1.1 — Docker Compose: Core Infrastructure](#step-11--docker-compose-core-infrastructure)
  - [Step 1.2 — Keycloak Realm Configuration](#step-12--keycloak-realm-configuration)
  - [Step 1.3 — API Gateway Service](#step-13--api-gateway-service)
- [Phase 2: Core Services](#phase-2-core-services)
  - [Step 2.1 — User Profile Service](#step-21--user-profile-service)
  - [Step 2.2 — Job Service: Domain Model & State Machine](#step-22--job-service-domain-model--state-machine)
  - [Step 2.3 — Job Service: Event Consumption](#step-23--job-service-event-consumption)
  - [Step 2.4 — Search Service](#step-24--search-service)
  - [Step 2.5 — Angular Frontend: Project Setup & Auth](#step-25--angular-frontend-project-setup--auth)
  - [Step 2.6 — Angular: Job Features](#step-26--angular-job-features)
  - [Step 2.7 — Angular: Profile & Search Pages](#step-27--angular-profile--search-pages)
- [Phase 3: Matching & Notifications](#phase-3-matching--notifications)
  - [Step 3.1 — Matching Service (Rule-Based)](#step-31--matching-service-rule-based)
  - [Step 3.2 — Notification Service](#step-32--notification-service)
  - [Step 3.3 — Angular: Notification Bell & Real-Time](#step-33--angular-notification-bell--real-time)
- [Phase 4: Payments & Messaging](#phase-4-payments--messaging)
  - [Step 4.1 — Payment Service: Stripe & Escrow](#step-41--payment-service-stripe--escrow)
  - [Step 4.2 — Angular: Payment UI with Stripe.js](#step-42--angular-payment-ui-with-stripejs)
  - [Step 4.3 — Messaging Service](#step-43--messaging-service)
  - [Step 4.4 — Angular: Chat Interface](#step-44--angular-chat-interface)
- [Phase 5: Observability, Resilience & Polish](#phase-5-observability-resilience--polish)
  - [Step 5.1 — Prometheus Metrics & Grafana Dashboards](#step-51--prometheus-metrics--grafana-dashboards)
  - [Step 5.2 — Distributed Tracing](#step-52--distributed-tracing)
  - [Step 5.3 — Resilience Patterns Audit](#step-53--resilience-patterns-audit)
  - [Step 5.4 — OpenAPI Documentation](#step-54--openapi-documentation)
  - [Step 5.5 — Error Handling & Validation Hardening](#step-55--error-handling--validation-hardening)
- [Phase 6: AI Matching & Final Deployment](#phase-6-ai-matching--final-deployment)
  - [Step 6.1 — AI Matching Microservice (Python/FastAPI)](#step-61--ai-matching-microservice-pythonfastapi)
  - [Step 6.2 — Nginx & Production Docker Compose](#step-62--nginx--production-docker-compose)
  - [Step 6.3 — Integration Testing](#step-63--integration-testing)
  - [Step 6.4 — Final README & Documentation](#step-64--final-readme--documentation)
  - [Step 6.5 — Git Tag & Push to GitHub](#step-65--git-tag--push-to-github)
- [Appendix: Complete Commit Log](#appendix-complete-commit-log)

---

# Phase 0: Project Initialization

> **Goal:** Monorepo created, Git initialized, shared module with all events and outbox pattern ready.

---

## Step 0.1 — Create Monorepo & Initialize Git

This creates the root directory structure, initializes Git, creates `CHANGELOG.md`, `.gitignore`, `.env.example`, and `README.md`.

### 📋 PROMPT

```text
Create a new project called "unikly" with this exact structure:

unikly/
├── backend/                    # Gradle multi-module (Java 25, Spring Boot 4.0.3)
│   ├── build.gradle           # Root Gradle build with shared deps
│   ├── settings.gradle        # Includes all submodules
│   ├── gradle.properties      # Java 21, Spring Boot 4.0.3
│   └── common/                # Shared module: DTOs, event classes, outbox base
│       └── build.gradle
├── frontend/                   # Angular 21 workspace (create later)
├── matching-ai/                # Python FastAPI (create later)
├── infra/
│   ├── docker/                # Dockerfiles per service
│   ├── keycloak/              # Realm export JSON
│   ├── prometheus/            # prometheus.yml
│   ├── grafana/               # Provisioned dashboards
│   └── nginx/                 # nginx.conf
├── .gitignore                  # Java, Node, Python, Docker, IDE, .env
├── .env.example                # All env vars documented (see below)
├── CHANGELOG.md                # Initialized with header and version 0.1.0
└── README.md                   # Project overview, tech stack table, setup instructions

For the root build.gradle, configure:
- Java 25 toolchain
- Spring Boot 4.0.3 plugin (apply false at root)
- Spring Dependency Management plugin
- Shared dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa,
  spring-boot-starter-validation, spring-boot-starter-actuator, spring-kafka,
  lombok, postgresql driver, resilience4j-spring-boot3, springdoc-openapi,
  jackson-databind, micrometer-registry-prometheus

The common module should have its own build.gradle with just the shared
event/DTO dependencies (no web starter).

.env.example must include these variables:
  DB_PASSWORD, KC_DB_PASSWORD, KC_ADMIN_USER, KC_ADMIN_PASSWORD,
  KAFKA_BOOTSTRAP_SERVERS, KEYCLOAK_URL, KEYCLOAK_REALM,
  STRIPE_SECRET_KEY, STRIPE_WEBHOOK_SECRET, STRIPE_PUBLISHABLE_KEY,
  REDIS_HOST, REDIS_PORT, ELASTICSEARCH_URL, FCM_SERVER_KEY, AI_SERVICE_URL

For CHANGELOG.md, use this exact format:

# Changelog

All notable changes to Unikly will be documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [0.1.0] - 2026-03-17

### Added
- Initialized monorepo structure with backend (Gradle multi-module), frontend,
  matching-ai, and infra directories
- Created root Gradle build with Spring Boot 4.0.3, Java 25 toolchain, and
  shared dependencies
- Created common module for shared DTOs and event classes
- Added .gitignore, .env.example, and README.md

Initialize git, add all files, and make the first commit.
```

### ✅ VERIFY

```bash
cd unikly && ./gradlew clean build
# common module should compile successfully
cat CHANGELOG.md   # should show [0.1.0] entry
cat .env.example   # should list all variables
```

### 📦 COMMIT

```bash
git commit -m "chore: initialize monorepo with Gradle multi-module, common module, and project skeleton"
```

### 📝 CHANGELOG

> `[0.1.0]` — Initial monorepo, Gradle config, common module, .gitignore, .env.example, README

---

## Step 0.2 — Common Module: Shared Events, DTOs, Outbox Base

Populates the common module with all shared classes that every service will import.

### 📋 PROMPT

```text
In the unikly/backend/common module, create the following packages and classes
under src/main/java/com/unikly/common/:

1. Package: events/
   Create these event record classes (Java records, immutable):

   - BaseEvent: { UUID eventId, String eventType, Instant timestamp }

   - JobCreatedEvent extends BaseEvent:
     { UUID jobId, UUID clientId, String title, String description,
       List<String> skills, Money budget, String status }

   - JobStatusChangedEvent extends BaseEvent:
     { UUID jobId, String previousStatus, String newStatus, UUID triggeredBy }

   - ProposalSubmittedEvent extends BaseEvent:
     { UUID jobId, UUID freelancerId, Money proposedBudget }

   - ProposalAcceptedEvent extends BaseEvent:
     { UUID jobId, UUID freelancerId, UUID clientId, Money agreedBudget }

   - PaymentInitiatedEvent extends BaseEvent:
     { UUID paymentId, UUID jobId, UUID clientId, Money amount, String providerRef }

   - PaymentCompletedEvent extends BaseEvent:
     { UUID paymentId, UUID jobId, String providerRef }

   - EscrowReleasedEvent extends BaseEvent:
     { UUID paymentId, UUID jobId, UUID freelancerId, Money amount }

   - JobMatchedEvent extends BaseEvent:
     { UUID jobId, List<MatchEntry> matches, String strategy }

   - MessageSentEvent extends BaseEvent:
     { UUID conversationId, UUID senderId, UUID recipientId, String preview }

   - UserProfileUpdatedEvent extends BaseEvent:
     { UUID userId, List<String> updatedFields, List<String> skills }

2. Package: dto/
   - Money: Java record { BigDecimal amount, String currency }
   - MatchEntry: Java record { UUID freelancerId, BigDecimal score, List<String> matchedSkills }
   - PageResponse<T>: Generic wrapper { List<T> content, int page, int size,
     long totalElements, int totalPages }

3. Package: outbox/
   - OutboxEvent entity (JPA @Entity, @Table(name="outbox_events")):
     Fields: UUID id (PK, auto-generated), String eventType,
     String payload (JSONB column using @Column(columnDefinition="jsonb")),
     OutboxStatus status (ENUM: PENDING, SENT, FAILED),
     Instant createdAt, Instant sentAt (nullable), int retryCount (default 0)

   - OutboxRepository: Spring Data JPA repository with method
     findByStatusOrderByCreatedAtAsc(OutboxStatus status)

   - OutboxPublisher: @Component that runs @Scheduled(fixedDelay=500) to poll
     PENDING events, publish to Kafka, mark SENT. Include try-catch: on failure
     increment retryCount, if retryCount >= 5 mark FAILED.

4. Package: security/
   - UserContext: Utility class that extracts userId, roles from
     SecurityContextHolder or X-User-Id / X-User-Roles headers

Ensure all events use Jackson @JsonTypeInfo for polymorphic deserialization.
Ensure the module compiles: ./gradlew :common:build

Update CHANGELOG.md under [Unreleased]:
### Added
- Shared event records (11 events) for inter-service communication
- Money, MatchEntry, PageResponse DTOs
- OutboxEvent JPA entity with scheduled publisher for guaranteed delivery
- UserContext security utility
```

### ✅ VERIFY

```bash
./gradlew :common:build
# Must succeed. Check: 11 event classes, 3 DTOs, outbox entity+repo+publisher, UserContext
find backend/common/src -name "*.java" | wc -l  # Should be ~18-20 files
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(common): add shared event classes, DTOs, outbox pattern, and security utilities"
```

### 📝 CHANGELOG

> **Added:** Shared event records (11 events), Money/MatchEntry/PageResponse DTOs, OutboxEvent JPA entity with publisher, UserContext utility

---

# Phase 1: Infrastructure Setup

> **Goal:** Docker Compose running all infrastructure, Keycloak configured, API Gateway deployed.

---

## Step 1.1 — Docker Compose: Core Infrastructure

Creates the Docker Compose file with all infrastructure services.

### 📋 PROMPT

```text
Create infra/docker-compose.yml with the following services. Use EXACT versions.
All services connect to a bridge network named "unikly-net".

INFRASTRUCTURE SERVICES:

1. postgres-keycloak:
   image: postgres:18
   port: 5439:5432
   volume: unikly-pg-keycloak-data:/var/lib/postgresql/data
   env: POSTGRES_DB=keycloak, POSTGRES_USER=keycloak,
        POSTGRES_PASSWORD=${KC_DB_PASSWORD}
   healthcheck: pg_isready -U keycloak

2. keycloak:
   image: quay.io/keycloak/keycloak:26.5.5
   port: 8180:8080
   command: "start-dev --import-realm"
   depends_on: postgres-keycloak (healthy)
   env: KC_DB=postgres, KC_DB_URL=jdbc:postgresql://postgres-keycloak:5432/keycloak,
        KC_DB_USERNAME=keycloak, KC_DB_PASSWORD=${KC_DB_PASSWORD},
        KEYCLOAK_ADMIN=${KC_ADMIN_USER}, KEYCLOAK_ADMIN_PASSWORD=${KC_ADMIN_PASSWORD},
        KC_HEALTH_ENABLED=true
   healthcheck: curl -f http://localhost:8080/health/ready || exit 1

3. kafka:
   image: apache/kafka:4.2.0
   port: 9092:9092
   KRaft mode (NO ZooKeeper). Environment:
     KAFKA_NODE_ID=1
     KAFKA_PROCESS_ROLES=broker,controller
     KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
     KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
     KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092
     KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER
     KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
     KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
     KAFKA_AUTO_CREATE_TOPICS_ENABLE=false
     CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk (or generate a base64 UUID)
   volume: unikly-kafka-data:/tmp/kraft-combined-logs

4. kafka-init:
   image: apache/kafka:4.2.0
   depends_on: kafka (healthy)
   entrypoint: /bin/sh -c
   command: shell script that creates these topics (replication-factor 1):
     - job.events (6 partitions)
     - payment.events (4 partitions)
     - matching.events (4 partitions)
     - messaging.events (6 partitions)
     - user.events (4 partitions)
     - notification.dlq (2 partitions)
     - payment.dlq (2 partitions)

5-10. PER-SERVICE DATABASES (all postgres:18, same pattern):
   postgres-jobs:         port 5433, DB=unikly_jobs
   postgres-users:        port 5434, DB=unikly_users
   postgres-payments:     port 5435, DB=unikly_payments
   postgres-matching:     port 5436, DB=unikly_matching
   postgres-messaging:    port 5437, DB=unikly_messaging
   postgres-notifications: port 5438, DB=unikly_notifications
   All use: POSTGRES_USER=unikly, POSTGRES_PASSWORD=${DB_PASSWORD}
   Each has its own named volume: unikly-pg-{name}-data
   Each has healthcheck: pg_isready -U unikly

11. elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:9.0.0
    port: 9200:9200
    env: discovery.type=single-node, xpack.security.enabled=false,
         ES_JAVA_OPTS="-Xms512m -Xmx512m"
    volume: unikly-es-data:/usr/share/elasticsearch/data
    healthcheck: curl -f http://localhost:9200/_cluster/health || exit 1

12. redis:
    image: redis:7.4
    port: 6379:6379
    volume: unikly-redis-data:/data
    healthcheck: redis-cli ping | grep -q PONG

ALSO create infra/docker-compose.monitoring.yml (separate file) with:

13. prometheus:
    image: prom/prometheus:latest
    port: 9090:9090
    volume: ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro

14. grafana:
    image: grafana/grafana:latest
    port: 3000:3000
    depends_on: prometheus
    env: GF_SECURITY_ADMIN_PASSWORD=admin

Create infra/prometheus/prometheus.yml:
  global scrape_interval: 15s
  scrape_configs for: gateway (8080), job-service (8081), user-service (8082),
    payment-service (8083), matching-service (8084), messaging-service (8085),
    notification-service (8086), search-service (8087)
  Each target: {service}:{port}/actuator/prometheus

Create a .env file in infra/ with sensible dev defaults:
  DB_PASSWORD=unikly_dev_2026
  KC_DB_PASSWORD=keycloak_dev_2026
  KC_ADMIN_USER=admin
  KC_ADMIN_PASSWORD=admin

Update CHANGELOG.md under [Unreleased].
```

### ✅ VERIFY

```bash
cd infra && docker compose up -d && docker compose ps
# All containers should be healthy within 90 seconds
# Verify:
curl http://localhost:9200          # Elasticsearch responds
redis-cli -p 6379 ping             # PONG
curl http://localhost:8180          # Keycloak login page
docker compose logs kafka-init     # Topics created successfully
```

### 📦 COMMIT

```bash
git add -A && git commit -m "infra: add Docker Compose with PostgreSQL (7 instances), Kafka 4.2 KRaft, Keycloak 26.5, Elasticsearch, Redis, Prometheus, Grafana"
```

### 📝 CHANGELOG

> **Added:** Docker Compose with all infrastructure — 7 PostgreSQL instances, Kafka 4.2.0 (KRaft, no ZooKeeper), Keycloak 26.5.5, Elasticsearch 9.0, Redis 7.4, Prometheus, Grafana. Kafka topics auto-created on startup.

---

## Step 1.2 — Keycloak Realm Configuration

### 📋 PROMPT

```text
Configure Keycloak for the Unikly project.

Create infra/keycloak/unikly-realm.json — a Keycloak realm export that defines:

Realm: "unikly"
- registrationAllowed: true
- loginWithEmailAllowed: true
- duplicateEmailsAllowed: false
- accessTokenLifespan: 300 (5 minutes)
- ssoSessionMaxLifespan: 2592000 (30 days)
- refreshTokenMaxReuse: 0

Clients:
1. "unikly-frontend"
   - publicClient: true
   - standardFlowEnabled: true
   - directAccessGrantsEnabled: false
   - PKCE enabled (pkce.code.challenge.method = S256)
   - redirectUris: ["http://localhost:4200/*"]
   - webOrigins: ["http://localhost:4200"]

2. "unikly-backend"
   - publicClient: false (confidential)
   - serviceAccountsEnabled: true
   - secret: "unikly-backend-secret" (for dev only)

Realm Roles:
- ROLE_CLIENT
- ROLE_FREELANCER
- ROLE_ADMIN

Update docker-compose.yml keycloak service:
- Mount: ./keycloak:/opt/keycloak/data/import
- The --import-realm flag should already be in the command

Also create infra/keycloak/setup-test-users.sh:
A bash script that uses Keycloak Admin REST API (curl) to create test users:
- client1@test.com / password123 → assigned ROLE_CLIENT
- freelancer1@test.com / password123 → assigned ROLE_FREELANCER
- admin@test.com / password123 → assigned ROLE_ADMIN

The script should:
1. Get admin token from Keycloak
2. Create each user via POST /admin/realms/unikly/users
3. Set password via PUT /admin/realms/unikly/users/{id}/reset-password
4. Assign role via POST /admin/realms/unikly/users/{id}/role-mappings/realm
5. Include usage instructions in comments at the top

Make the script executable: chmod +x setup-test-users.sh

Update CHANGELOG.md.
```

### ✅ VERIFY

```bash
cd infra && docker compose down && docker compose up -d
# Wait for Keycloak to be healthy (~30s)
# Open http://localhost:8180/admin — login with admin/admin
# Verify: realm "unikly" exists, both clients visible, 3 roles defined
# Run: ./keycloak/setup-test-users.sh
# Verify: 3 test users created in Keycloak admin console
```

### 📦 COMMIT

```bash
git add -A && git commit -m "infra(keycloak): add realm export with clients, roles, and test user setup script"
```

### 📝 CHANGELOG

> **Added:** Keycloak realm `unikly` with frontend (PKCE) and backend (confidential) clients, ROLE_CLIENT/ROLE_FREELANCER/ROLE_ADMIN roles, test user creation script

---

## Step 1.3 — API Gateway Service

### 📋 PROMPT

```text
Create a new Gradle submodule: backend/gateway/

This is a Spring Cloud Gateway application (Spring Boot 4.0.3, Java 21).

build.gradle dependencies:
- spring-cloud-starter-gateway (use Spring Cloud 2024.0.x BOM compatible with Boot 4.0)
- spring-boot-starter-oauth2-resource-server
- spring-boot-starter-actuator
- spring-boot-starter-data-redis-reactive (for rate limiting)
- micrometer-registry-prometheus
- resilience4j-spring-boot3

application.yml configuration:

server.port: 8080

Spring Cloud Gateway routes:
  - id: job-service
    uri: http://job-service:8081
    predicates: Path=/api/jobs/**

  - id: user-service
    uri: http://user-service:8082
    predicates: Path=/api/users/**

  - id: payment-service
    uri: http://payment-service:8083
    predicates: Path=/api/payments/**

  - id: matching-service
    uri: http://matching-service:8084
    predicates: Path=/api/matches/**

  - id: messaging-service
    uri: http://messaging-service:8085
    predicates: Path=/api/messages/**

  - id: notification-service
    uri: http://notification-service:8086
    predicates: Path=/api/notifications/**

  - id: search-service
    uri: http://search-service:8087
    predicates: Path=/api/search/**

  - id: webhooks (NO auth filter on this route)
    uri: http://payment-service:8083
    predicates: Path=/webhooks/**

  - id: ws-notifications
    uri: ws://notification-service:8086
    predicates: Path=/ws/**

OAuth2 Resource Server:
  jwt.issuer-uri: ${KEYCLOAK_URL:http://keycloak:8080}/realms/unikly

CORS:
  allowed-origins: http://localhost:4200
  allowed-methods: "*"
  allowed-headers: "*"
  allow-credentials: true

Custom components:
1. JwtHeaderRelayFilter (GlobalFilter): Extracts 'sub' (userId) and
   'realm_access.roles' from JWT claims, adds X-User-Id and X-User-Roles
   headers to downstream requests. Skips /webhooks/** routes.

2. Rate limiting: RequestRateLimiter filter using Redis.
   Key resolver: extract userId from JWT. Default: 100 requests/min per user.

3. Circuit breaker: Resilience4j per route. On failure: return 503 with
   JSON body { "error": "Service temporarily unavailable", "service": "{name}" }

Actuator: expose health, prometheus, info, circuitbreakers endpoints.

Create infra/docker/Dockerfile.gateway:
  Stage 1: gradle:8.14-jdk21 — copy source, run gradle :gateway:bootJar
  Stage 2: eclipse-temurin:21-jre-jammy — copy jar, expose 8080, ENTRYPOINT java -jar

Add gateway to docker-compose.yml:
  port: 8080:8080
  depends_on: keycloak (healthy), redis (healthy)
  env: KEYCLOAK_URL=http://keycloak:8080, REDIS_HOST=redis
  healthcheck: curl -f http://localhost:8080/actuator/health || exit 1

Update backend/settings.gradle to include 'gateway'.
Update CHANGELOG.md.
```

### ✅ VERIFY

```bash
./gradlew :gateway:build                 # Compiles successfully
cd infra && docker compose build gateway  # Docker build succeeds
docker compose up -d gateway
curl http://localhost:8080/actuator/health  # {"status":"UP"}
curl http://localhost:8080/api/jobs         # 401 Unauthorized (no JWT)
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(gateway): implement API Gateway with JWT validation, routing, rate limiting, and circuit breakers"
```

### 📝 CHANGELOG

> **Added:** Spring Cloud Gateway with Keycloak JWT validation, route configuration for all 7 services + webhooks, Redis rate limiting (100/min per user), Resilience4j circuit breakers, CORS config, Dockerfile

---

# Phase 2: Core Services

> **Goal:** Job posting, profile management, proposals, search, and Angular frontend all working.

---

## Step 2.1 — User Profile Service

### 📋 PROMPT

```text
Create backend/user-service/ as a Spring Boot 4.0.3 Gradle submodule.

Package structure: com.unikly.userservice.{domain, application, infrastructure, api}

DOMAIN LAYER:

UserProfile entity (JPA @Entity):
  - id: UUID (PK, same as Keycloak userId — set manually, not auto-generated)
  - displayName: String @NotBlank @Size(max=100)
  - bio: String (TEXT column)
  - avatarUrl: String
  - role: UserRole enum (CLIENT, FREELANCER)
  - skills: List<String> (mapped as TEXT[] PostgreSQL array, FREELANCER only)
  - hourlyRate: BigDecimal (nullable, FREELANCER only)
  - currency: String (VARCHAR 3)
  - location: String
  - portfolioLinks: List<String> (TEXT[] array)
  - averageRating: BigDecimal (default 0, 2 decimal places)
  - totalReviews: int (default 0)
  - createdAt: Instant
  - updatedAt: Instant
  - version: int (@Version for optimistic locking)

Review entity:
  - id: UUID (PK, auto-generated)
  - reviewerId: UUID
  - revieweeId: UUID
  - jobId: UUID
  - rating: int @Min(1) @Max(5)
  - comment: String @Size(max=2000)
  - createdAt: Instant

APPLICATION LAYER:

UserProfileService:
  - createOrUpdateProfile(userId, dto): creates if not exists, updates if exists
  - getProfile(userId): returns profile or 404
  - searchFreelancers(skill, page, size): filter by role=FREELANCER and skill
  - On profile/skill change: publish UserProfileUpdatedEvent via outbox

ReviewService:
  - createReview(reviewerId, revieweeId, jobId, rating, comment):
    Validates that reviewer is not reviewing themselves.
    After saving, recalculate averageRating and totalReviews on the reviewee's profile.
  - getReviews(userId, page, size)

INFRASTRUCTURE LAYER:

  - UserProfileRepository (Spring Data JPA)
  - ReviewRepository
  - KafkaEventPublisher: serializes events, publishes to "user.events"

API LAYER:

  All endpoints (REST controllers with request/response DTOs):
  - GET    /api/users/me              → current user's profile
  - PUT    /api/users/me              → update current user's profile
  - GET    /api/users/{id}            → any user's public profile
  - GET    /api/users?role=FREELANCER&skill=Angular&page=0&size=20
  - POST   /api/users/{id}/reviews    → submit a review
  - GET    /api/users/{id}/reviews?page=0&size=10

  GlobalExceptionHandler:
  - MethodArgumentNotValidException → 400
  - EntityNotFoundException → 404
  - OptimisticLockingFailureException → 409
  - Generic → 500 with traceId

application.yml:
  server.port: 8082
  spring.datasource.url: jdbc:postgresql://postgres-users:5432/unikly_users
  spring.datasource.username: unikly
  spring.datasource.password: ${DB_PASSWORD}
  spring.jpa.hibernate.ddl-auto: update
  spring.kafka.bootstrap-servers: kafka:9092

Create infra/docker/Dockerfile.user-service (same multi-stage pattern as gateway).
Add user-service to docker-compose.yml: port 8082, depends_on postgres-users, kafka.
Update backend/settings.gradle. Update CHANGELOG.md.
```

### ✅ VERIFY

```bash
./gradlew :user-service:build
cd infra && docker compose up -d user-service
# Get a JWT token from Keycloak for client1@test.com
# PUT /api/users/me with profile data → 200
# GET /api/users/me → returns profile
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(user-service): implement User Profile Service with CRUD, reviews, Kafka events, and outbox pattern"
```

### 📝 CHANGELOG

> **Added:** User Profile Service — profile CRUD, skills management, reviews with automatic rating recalculation, UserProfileUpdatedEvent via outbox, Dockerfile, Docker Compose integration

---

## Step 2.2 — Job Service: Domain Model & State Machine

### 📋 PROMPT

```text
Create backend/job-service/ as a Spring Boot 4.0.3 Gradle submodule.
This is the MOST CRITICAL service. Follow the SRS Section 5.4 precisely.

Package: com.unikly.jobservice.{domain, application, infrastructure, api}

DOMAIN LAYER:

JobStatus enum: DRAFT, OPEN, IN_PROGRESS, COMPLETED, CLOSED, CANCELLED, DISPUTED, REFUNDED

Job entity:
  - id: UUID PK (auto-generated)
  - clientId: UUID @NotNull
  - title: String @NotBlank @Size(max=200)
  - description: String @NotBlank (TEXT column)
  - budget: BigDecimal @NotNull @Positive @Digits(integer=10, fraction=2)
  - currency: String @NotBlank @Size(max=3)
  - skills: List<String> @NotEmpty (PostgreSQL TEXT[])
  - status: JobStatus (default DRAFT)
  - createdAt, updatedAt: Instant
  - version: int @Version

ProposalStatus enum: PENDING, ACCEPTED, REJECTED, WITHDRAWN

Proposal entity:
  - id: UUID PK
  - jobId: UUID (FK to Job, indexed)
  - freelancerId: UUID
  - proposedBudget: BigDecimal @Positive
  - coverLetter: String @Size(max=5000)
  - status: ProposalStatus (default PENDING)
  - createdAt: Instant
  - version: int @Version

ContractStatus enum: ACTIVE, COMPLETED, TERMINATED

Contract entity:
  - id: UUID PK
  - jobId: UUID (FK, unique — one contract per job)
  - clientId: UUID
  - freelancerId: UUID
  - agreedBudget: BigDecimal
  - terms: String (TEXT)
  - status: ContractStatus (default ACTIVE)
  - startedAt: Instant
  - completedAt: Instant (nullable)

JobStatusMachine class:
  Private static final Map<JobStatus, Set<JobStatus>> VALID_TRANSITIONS:
    DRAFT → {OPEN}
    OPEN → {IN_PROGRESS, CANCELLED}
    IN_PROGRESS → {COMPLETED, DISPUTED}
    COMPLETED → {CLOSED}
    DISPUTED → {COMPLETED, REFUNDED}

  Method: void validateTransition(JobStatus current, JobStatus target)
    throws InvalidStatusTransitionException

APPLICATION LAYER:

JobService:
  - createJob(clientId, dto): creates job, publishes JobCreatedEvent if status=OPEN
  - getJob(id): returns job with proposal count
  - listJobs(status, skill, minBudget, maxBudget, sort, page, size):
    Supports all filter combinations. Uses Spring Data Specifications or
    Querydsl for dynamic filtering.
  - updateJob(id, clientId, dto): only allowed if status=DRAFT
  - transitionStatus(id, userId, newStatus): validates via JobStatusMachine,
    publishes JobStatusChangedEvent

ProposalService:
  - submitProposal(jobId, freelancerId, dto):
    Validates job is OPEN, freelancer != job client.
    Publishes ProposalSubmittedEvent.
  - acceptProposal(jobId, proposalId, clientId):
    Validates caller is job owner. Sets proposal to ACCEPTED.
    Rejects all other PENDING proposals for this job.
    Creates Contract entity. Publishes ProposalAcceptedEvent.
  - rejectProposal(jobId, proposalId, clientId)
  - listProposals(jobId, clientId): only job owner can see all proposals

API LAYER:

  POST   /api/jobs                                    → create job
  GET    /api/jobs?status=OPEN&skill=Angular&minBudget=100&maxBudget=5000&sort=createdAt,desc&page=0&size=20
  GET    /api/jobs/{id}                               → job details + proposal count
  PATCH  /api/jobs/{id}                               → update (DRAFT only)
  PATCH  /api/jobs/{id}/status                        → transition status
  POST   /api/jobs/{id}/proposals                     → submit proposal (FREELANCER)
  GET    /api/jobs/{id}/proposals                     → list proposals (JOB OWNER)
  PATCH  /api/jobs/{id}/proposals/{pid}/accept        → accept proposal
  PATCH  /api/jobs/{id}/proposals/{pid}/reject        → reject proposal

  Authorization:
  - POST /api/jobs: X-User-Roles must contain ROLE_CLIENT
  - POST /api/jobs/{id}/proposals: X-User-Roles must contain ROLE_FREELANCER
  - GET proposals, accept, reject: X-User-Id must match job.clientId

Database indexes (create via @Table(indexes=...) or Flyway):
  - idx_job_status ON jobs(status)
  - idx_job_client ON jobs(client_id)
  - idx_job_created ON jobs(created_at DESC)
  - idx_job_budget ON jobs(budget)
  - idx_job_skills ON jobs USING GIN(skills)  ← use native SQL for GIN

application.yml: port 8081, postgres-jobs:5432/unikly_jobs, kafka:9092

Create Dockerfile. Add to docker-compose.yml. Update settings.gradle.
Update CHANGELOG.md.
```

### ✅ VERIFY

```bash
./gradlew :job-service:build
# Full lifecycle test:
# 1. POST /api/jobs → creates DRAFT
# 2. PATCH /api/jobs/{id}/status {"status":"OPEN"} → 200
# 3. POST /api/jobs/{id}/proposals (as freelancer) → 201
# 4. PATCH /api/jobs/{id}/proposals/{pid}/accept (as client) → 200
# 5. Verify contract created
# 6. PATCH /api/jobs/{id}/status {"status":"COMPLETED"} from OPEN → 400 (invalid transition)
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(job-service): implement Job Service with lifecycle state machine, proposals, contracts, and event publishing"
```

### 📝 CHANGELOG

> **Added:** Job Service — full lifecycle state machine (DRAFT→CLOSED), proposal workflow with accept/reject, contract auto-creation, pagination/filtering with Specifications, JobCreatedEvent/JobStatusChangedEvent/ProposalSubmittedEvent via outbox, GIN index on skills array

---

## Step 2.3 — Job Service: Event Consumption

### 📋 PROMPT

```text
Add Kafka event consumption to the existing Job Service (backend/job-service).

Create in infrastructure layer:

1. PaymentEventConsumer (@Component, @KafkaListener):
   - Listen to "payment.events" topic, group "job-service-group"
   - Handle PaymentCompletedEvent:
     Find job by jobId. If job status is OPEN and has an accepted proposal,
     transition status to IN_PROGRESS. Publish JobStatusChangedEvent.
   - Handle EscrowReleasedEvent:
     Log the event. Mark internal payment tracking as settled.

2. Idempotent consumer infrastructure:
   - Create ProcessedEvent entity: { eventId UUID PK, processedAt Instant }
   - Create ProcessedEventRepository
   - In each consumer method, BEFORE processing:
     Check if eventId exists in processed_events. If yes, log "duplicate, skipping" and return.
     If no, process the event and save eventId in the SAME @Transactional block.

3. Error handling configuration in application.yml:
   spring.kafka.consumer:
     auto-offset-reset: earliest
     enable-auto-commit: false
   spring.kafka.listener:
     ack-mode: RECORD
     concurrency: 3

4. Dead Letter Queue: Configure Spring Kafka DefaultErrorHandler with
   FixedBackOff(1000, 3) — retry 3 times with 1s delay. After max retries,
   use DeadLetterPublishingRecoverer to send to "job-service.dlq" topic.

Update CHANGELOG.md.
```

### ✅ VERIFY

```bash
# Publish a test PaymentCompletedEvent to payment.events topic using kafka-console-producer
# Verify job status transitions to IN_PROGRESS
# Publish the same event again — verify it's skipped (check logs)
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(job-service): add Kafka consumers for PaymentCompletedEvent and EscrowReleasedEvent with idempotent processing"
```

### 📝 CHANGELOG

> **Added:** Job Service Kafka consumers for PaymentCompletedEvent (auto-transitions to IN_PROGRESS) and EscrowReleasedEvent, idempotent consumer pattern with processed_events table, DLQ routing after 3 retries

---

## Step 2.4 — Search Service

### 📋 PROMPT

```text
Create backend/search-service/ as a Spring Boot 4.0.3 Gradle submodule.

Package: com.unikly.searchservice.{domain, application, infrastructure, api}

Dependencies: spring-boot-starter-data-elasticsearch, spring-kafka,
spring-boot-starter-web, spring-boot-starter-actuator, micrometer-registry-prometheus

DOMAIN:

JobDocument (@Document(indexName = "jobs")):
  jobId (keyword), title (text, analyzed), description (text, analyzed),
  skills (keyword array), budget (double), currency (keyword),
  status (keyword), clientId (keyword), createdAt (date)

FreelancerDocument (@Document(indexName = "freelancers")):
  userId (keyword), displayName (text), skills (keyword array),
  hourlyRate (double), averageRating (double), location (text), bio (text, analyzed)

INFRASTRUCTURE:

Kafka consumers (idempotent pattern for all):
  - "job.events" group "search-service-group":
    JobCreatedEvent → index new JobDocument
    JobStatusChangedEvent → update status, DELETE from index if CANCELLED or CLOSED
  - "user.events" group "search-service-group":
    UserProfileUpdatedEvent → re-index FreelancerDocument (fetch full profile via
    internal REST call to user-service, or just use the event data)

APPLICATION:

SearchService:
  - searchJobs(query, skills, minBudget, maxBudget, page, size):
    Use NativeQuery with BoolQuery:
      must: multi_match on title + description (if query provided)
      filter: terms on skills (if provided), range on budget (if provided),
              term status=OPEN (always filter to OPEN jobs)
    Sort by _score desc, then createdAt desc
    Return PageResponse<JobSearchResult>

  - searchFreelancers(query, skills, minRating, page, size):
    Similar multi_match on displayName + bio + skills
    Filter: terms on skills, range on averageRating

  - getSuggestions(prefix, type):
    If type=skill: use a hardcoded set initially, or aggregation on skills field
    Return List<String> of matching suggestions

API:
  GET /api/search/jobs?q=angular+dashboard&skills=Angular,TypeScript&minBudget=100&maxBudget=5000&page=0&size=20
  GET /api/search/freelancers?q=java+developer&skills=Java,Spring&minRating=4&page=0&size=20
  GET /api/search/suggestions?q=ang&type=skill

application.yml: port 8087, spring.elasticsearch.uris=http://elasticsearch:9200, kafka:9092

Create Dockerfile. Add to docker-compose.yml (depends_on: elasticsearch, kafka).
Update settings.gradle. Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(search-service): implement Search Service with Elasticsearch indexing, full-text search, and Kafka event sync"
```

### 📝 CHANGELOG

> **Added:** Search Service — Elasticsearch 9 integration, jobs and freelancers indexes, full-text search with BM25, faceted filtering by skills/budget/rating, autocomplete suggestions, Kafka consumers for real-time index sync

---

## Step 2.5 — Angular Frontend: Project Setup & Auth

### 📋 PROMPT

```text
Create the Angular 19 frontend.

cd unikly/frontend
ng new unikly-app --style=scss --routing --standalone --skip-git

Install:
  npm install keycloak-js @stomp/stompjs
  ng add @angular/material  (choose a theme, set up typography and animations)

Project structure:

src/app/
  core/
    auth/
      keycloak.service.ts
        - Initialize Keycloak: url=environment.keycloak.url, realm='unikly',
          clientId='unikly-frontend'
        - init(): Keycloak.init({ onLoad: 'check-sso', pkceMethod: 'S256',
          silentCheckSsoRedirectUri: window.location.origin + '/assets/silent-check-sso.html' })
        - login(), logout(), getToken(), isAuthenticated(), getUserId(), getRoles(),
          hasRole(role)
      auth.guard.ts — CanActivateFn, redirects to login if not authenticated
      role.guard.ts — CanActivateFn, checks if user has required role
      auth.interceptor.ts — HttpInterceptorFn:
        Attaches "Authorization: Bearer {token}" to all requests matching /api/**
        On 401: attempt Keycloak.updateToken(30). If success, retry request.
        If fail, redirect to login.
      error.interceptor.ts — HttpInterceptorFn:
        Catches errors. Shows MatSnackBar for 4xx (user error) and 5xx (server error).
        Does NOT intercept 401 (handled by auth interceptor).
    services/
      api.service.ts — generic typed HTTP methods using HttpClient

  features/
    auth/     → placeholder
    jobs/     → placeholder components (job-list, job-create, job-detail)
    profile/  → placeholder
    search/   → placeholder
    messaging/ → placeholder
    notifications/ → placeholder
    payments/ → placeholder

  shared/
    components/
      loading-spinner/ — simple spinner overlay
    pipes/
      time-ago.pipe.ts — transforms Instant/Date to "2 hours ago" format
    directives/

  layouts/
    main-layout/
      main-layout.component.ts — MatSidenav + MatToolbar
        Sidebar: nav links (Jobs, Search, Messages, Profile)
        Toolbar: app name, notification bell placeholder, user menu (profile, logout)
    auth-layout/
      auth-layout.component.ts — centered card for login/register redirects

app.routes.ts:
  '' → redirect to '/jobs'
  'jobs' → lazy load job-list (guard: authGuard)
  'jobs/create' → lazy load job-create (guard: authGuard, roleGuard('ROLE_CLIENT'))
  'jobs/:id' → lazy load job-detail (guard: authGuard)
  'profile' → lazy load profile (guard: authGuard)
  'users/:id' → lazy load public-profile
  'search' → lazy load search
  'messages' → lazy load messaging (guard: authGuard)
  'messages/:id' → lazy load conversation
  'notifications' → lazy load notification-list

app.config.ts:
  provideHttpClient(withInterceptors([authInterceptor, errorInterceptor]))
  provideRouter(routes)
  provideAnimations()

environment.ts:
  apiUrl: 'http://localhost:8080/api'
  wsUrl: 'ws://localhost:8080/ws'
  keycloak: { url: 'http://localhost:8180', realm: 'unikly', clientId: 'unikly-frontend' }

Create src/assets/silent-check-sso.html (standard Keycloak silent SSO check page).

Create infra/docker/Dockerfile.frontend:
  Stage 1: node:22 — npm ci && ng build --configuration production
  Stage 2: nginx:1.27-alpine — copy dist output to /usr/share/nginx/html,
           copy nginx.conf

Create infra/nginx/nginx.conf:
  - Listen 80
  - location / → try_files $uri $uri/ /index.html (SPA routing)
  - location /api/ → proxy_pass http://gateway:8080/api/
  - location /ws/ → proxy_pass http://gateway:8080/ws/ with WebSocket upgrade headers
  - gzip on for text/html, application/javascript, text/css, application/json

Update CHANGELOG.md.
```

### ✅ VERIFY

```bash
cd frontend/unikly-app && ng build  # Build succeeds
ng serve                            # Opens on localhost:4200
# Click any protected route → redirects to Keycloak login
# Login as client1@test.com → redirects back, sees main layout
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(frontend): initialize Angular 19 with Keycloak auth, routing, interceptors, and layout components"
```

### 📝 CHANGELOG

> **Added:** Angular 19 frontend with Keycloak PKCE auth, auth/role guards, JWT interceptor with silent refresh, error interceptor with snackbar, main layout with sidenav, feature module placeholders, Nginx config with SPA routing and WebSocket proxy, Dockerfile

---

## Step 2.6 — Angular: Job Features

### 📋 PROMPT

```text
Implement the job feature module in the Angular frontend (unikly/frontend/unikly-app).

1. features/jobs/services/job.service.ts
   Methods (all return Observable):
   - getJobs(params: {status?, skill?, minBudget?, maxBudget?, sort?, page, size})
     → GET /api/search/jobs (uses search service for full-text)
   - getMyJobs(page, size) → GET /api/jobs?clientId={currentUserId}
   - getJob(id) → GET /api/jobs/{id}
   - createJob(data) → POST /api/jobs
   - updateJobStatus(id, status) → PATCH /api/jobs/{id}/status
   - submitProposal(jobId, data) → POST /api/jobs/{id}/proposals
   - getProposals(jobId) → GET /api/jobs/{id}/proposals
   - acceptProposal(jobId, proposalId) → PATCH /api/jobs/{id}/proposals/{pid}/accept
   - rejectProposal(jobId, proposalId) → PATCH /api/jobs/{id}/proposals/{pid}/reject

2. features/jobs/job-list/job-list.component.ts (standalone)
   - Mat-card grid layout: 3 cols desktop, 2 tablet, 1 mobile (use CSS grid with media queries)
   - Each card: title, budget with currency, skills as MatChip list, status as colored MatBadge,
     "posted X ago" using timeAgo pipe, "View Details" button
   - Filter panel (collapsible on mobile):
     Status dropdown (All, Open, In Progress, Completed)
     Skills: MatChipInput with autocomplete (GET /api/search/suggestions?type=skill)
     Budget range: two number inputs (min/max)
     Search text input with 300ms debounce
   - MatPaginator at bottom
   - Empty state: "No jobs found. Try adjusting your filters."

3. features/jobs/job-create/job-create.component.ts (standalone)
   - Reactive form (FormBuilder):
     title: required, maxLength 200
     description: required, textarea
     budget: required, positive number
     currency: dropdown (USD, EUR, XAF, GBP)
     skills: MatChipInput with autocomplete, at least 1 required
   - Submit button: calls createJob, on success navigate to /jobs/{id}
     with MatSnackBar "Job posted successfully!"
   - Show validation errors inline below each field

4. features/jobs/job-detail/job-detail.component.ts (standalone)
   - Header: title, status badge, budget, posted by, date
   - Description section
   - Skills chips
   - Tab group:
     Tab 1 "Details": full description
     Tab 2 "Proposals" (only visible to job owner):
       List of proposals: freelancer name (link to /users/{id}), proposed budget,
       cover letter preview, accept/reject buttons
       Accept opens confirmation dialog
     Tab 3 "Matches" (visible to job owner):
       GET /api/matches?jobId={id} — show matched freelancers with scores
   - If current user is FREELANCER and job is OPEN:
     "Submit Proposal" FAB button → opens MatDialog with proposedBudget input
     and coverLetter textarea

Update app.routes.ts with proper lazy loading. Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(frontend): implement job list, create, and detail pages with proposal workflow"
```

### 📝 CHANGELOG

> **Added:** Job list page with search/filter/pagination, job creation form with validation, job detail with tabs (details, proposals, matches), proposal submission dialog, responsive Material card grid

---

## Step 2.7 — Angular: Profile & Search Pages

### 📋 PROMPT

```text
Implement profile and search features in the Angular frontend.

1. features/profile/profile.component.ts (standalone)
   - On init: GET /api/users/me
   - View mode: displays all profile fields nicely
   - Edit mode (toggle button): reactive form with:
     displayName, bio (textarea), skills (MatChipInput), hourlyRate, currency,
     location, portfolioLinks (dynamic FormArray — add/remove URL inputs)
   - Save: PUT /api/users/me → snackbar "Profile updated"
   - Reviews tab: GET /api/users/{myId}/reviews with star display and pagination
   - Stats: average rating (star component), total reviews, member since

2. features/profile/public-profile.component.ts (standalone)
   - Route: /users/:id
   - GET /api/users/{id} → read-only display
   - If freelancer: skills chips, hourly rate, portfolio links (clickable)
   - Reviews section with pagination
   - "Send Message" button → creates/opens conversation (Phase 4)

3. features/search/search.component.ts (standalone)
   - MatTabGroup: "Jobs" tab and "Freelancers" tab
   - Search input at top (shared between tabs), 300ms debounce
   - Jobs tab:
     GET /api/search/jobs?q={query}&skills=...&minBudget=...
     Results as condensed cards, click navigates to /jobs/{id}
   - Freelancers tab:
     GET /api/search/freelancers?q={query}&skills=...&minRating=...
     Results as user cards with avatar, name, skills, rating, click → /users/{id}
   - Filter chips for skills (addable/removable)

4. shared/components/ (create these reusable standalone components):
   - star-rating.component.ts:
     Input: rating (number 0-5), interactive (boolean)
     Display: 5 star icons, filled/empty based on rating
     Output: ratingChange event (when interactive)
   - skill-chips.component.ts:
     Input: skills (string[]), editable (boolean)
     Display: MatChip list
   - user-avatar.component.ts:
     Input: avatarUrl, displayName
     Display: image if url exists, otherwise first initial in colored circle

Update routes. Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(frontend): implement profile management, public profiles, unified search, and shared components"
```

### 📝 CHANGELOG

> **Added:** Profile page with edit mode and reviews, public profile view, unified search page with tabs (jobs/freelancers), reusable star-rating, skill-chips, and user-avatar components

---

# Phase 3: Matching & Notifications

> **Goal:** Intelligent matching engine and real-time notification system operational.

---

## Step 3.1 — Matching Service (Rule-Based)

### 📋 PROMPT

```text
Create backend/matching-service/ as a Spring Boot 4.0.3 Gradle submodule.

Package: com.unikly.matchingservice.{domain, application, infrastructure, api}

DOMAIN:

MatchResult entity:
  - id: UUID PK
  - jobId: UUID (indexed)
  - freelancerId: UUID (indexed)
  - score: BigDecimal(5,4) (e.g., 0.8723)
  - matchedSkills: List<String> (TEXT[])
  - strategy: MatchStrategy enum (RULE_BASED, AI_EMBEDDING)
  - createdAt: Instant

FreelancerSkillCache entity (local read-model):
  - userId: UUID PK
  - skills: List<String>
  - hourlyRate: BigDecimal
  - averageRating: BigDecimal
  - updatedAt: Instant

APPLICATION:

MatchingEngine interface: List<MatchResult> computeMatches(UUID jobId, String title,
  String description, List<String> requiredSkills, BigDecimal budget)

RuleBasedMatchingEngine implements MatchingEngine:
  1. Fetch all FreelancerSkillCache entries (or filter by at least 1 skill overlap)
  2. For each freelancer, compute:
     - skillOverlap = (count of matching skills / count of required skills) × 0.40
     - budgetCompat = max(0, 1 - abs(freelancer.hourlyRate × 160 - budget) / budget) × 0.20
       (160 = estimated hours for project, adjust as needed)
     - ratingScore = (freelancer.averageRating / 5.0) × 0.25
     - responseScore = 0.5 × 0.15 (placeholder, hardcoded)
     - finalScore = skillOverlap + budgetCompat + ratingScore + responseScore
       Clamp to [0.0, 1.0]
  3. Sort by score DESC, return top 20

MatchingService:
  - processJobForMatching(event: JobCreatedEvent):
    Run matching, store results, publish JobMatchedEvent
  - getMatchesForJob(jobId, limit): sorted by score DESC
  - getMatchesForFreelancer(freelancerId, limit): jobs matched to this freelancer

INFRASTRUCTURE:

Kafka consumer on "job.events" (group: matching-service-group):
  On JobCreatedEvent:
    - Check idempotency (processed_events table)
    - Schedule matching with 30-second delay using ScheduledExecutorService
    - This batches nearby events and reduces load

Kafka consumer on "user.events" (group: matching-service-group):
  On UserProfileUpdatedEvent:
    - Upsert FreelancerSkillCache with new skills, hourlyRate, averageRating

Kafka producer: JobMatchedEvent to "matching.events" via outbox

Resilience4j config for future AI service call:
  resilience4j.circuitbreaker.instances.ai-matching:
    failureRateThreshold: 50
    waitDurationInOpenState: 30s
    slidingWindowSize: 10

API:
  GET /api/matches?jobId={uuid}&limit=20
  GET /api/matches?freelancerId={uuid}&limit=10
  GET /api/matches/{id}

application.yml: port 8084, postgres-matching:5432/unikly_matching, kafka:9092

Create Dockerfile. Add to docker-compose.yml. Update settings.gradle.
Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(matching-service): implement rule-based matching engine with async processing, skill cache, and Kafka event flow"
```

### 📝 CHANGELOG

> **Added:** Matching Service — rule-based scoring (skill overlap 40%, budget 20%, rating 25%, response 15%), async matching with 30s batch delay, FreelancerSkillCache populated from Kafka events, JobMatchedEvent publishing, Resilience4j circuit breaker pre-configured for AI service

---

## Step 3.2 — Notification Service

### 📋 PROMPT

```text
Create backend/notification-service/ as a Spring Boot 4.0.3 Gradle submodule.

Dependencies: spring-boot-starter-websocket, spring-boot-starter-data-redis,
spring-kafka, spring-boot-starter-web, spring-boot-starter-data-jpa,
spring-boot-starter-actuator, micrometer-registry-prometheus

Package: com.unikly.notificationservice.{domain, application, infrastructure, api}

DOMAIN:

NotificationType enum: JOB_MATCHED, PROPOSAL_RECEIVED, PROPOSAL_ACCEPTED,
  PAYMENT_FUNDED, ESCROW_RELEASED, MESSAGE_RECEIVED, SYSTEM

Notification entity:
  - id: UUID PK
  - userId: UUID (indexed)
  - type: NotificationType
  - title: String @Size(max=200)
  - body: String (TEXT)
  - read: boolean (default false)
  - actionUrl: String (nullable, e.g., "/jobs/uuid-123")
  - createdAt: Instant

NotificationPreference entity:
  - userId: UUID PK
  - emailEnabled: boolean (default true)
  - pushEnabled: boolean (default true)
  - realtimeEnabled: boolean (default true)
  - quietHoursStart: LocalTime (nullable)
  - quietHoursEnd: LocalTime (nullable)

APPLICATION:

NotificationDeliveryService:
  Method: createAndDeliver(userId, type, title, body, actionUrl)
    1. Save Notification to DB
    2. Load NotificationPreference for user (or use defaults)
    3. If realtimeEnabled:
       Check Redis key "user:{userId}:online" (TTL-based presence)
       If exists → send via WebSocket (STOMP to /user/{userId}/queue/notifications)
    4. If NOT online AND pushEnabled:
       Log "Would send FCM push to {userId}" (placeholder for Firebase integration)
    5. Return saved notification

INFRASTRUCTURE:

WebSocket configuration:
  - Enable STOMP over WebSocket
  - Endpoint: /ws/notifications (SockJS fallback)
  - Application destination prefix: /app
  - User destination prefix: /user
  - On CONNECT frame: extract JWT from query param "token", validate with Keycloak
    public key, set user principal
  - On CONNECT: set Redis key "user:{userId}:online" = sessionId, TTL 120s
  - Schedule heartbeat handler: refresh Redis TTL every 60s for active connections
  - On DISCONNECT: delete Redis key

Kafka consumers (ALL use idempotent pattern):
  "job.events" (group: notification-service-group):
    - ProposalSubmittedEvent → notify job.clientId: "New proposal on your job '{title}'"
    - ProposalAcceptedEvent → notify freelancerId: "Your proposal was accepted!"

  "payment.events" (group: notification-service-group):
    - PaymentCompletedEvent → notify freelancerId: "Escrow funded for your job"
    - EscrowReleasedEvent → notify freelancerId: "Payment of {amount} has been released!"

  "matching.events" (group: notification-service-group):
    - JobMatchedEvent → notify job.clientId: "We found {count} freelancers for your job"

  "messaging.events" (group: notification-service-group):
    - MessageSentEvent → notify recipientId: "New message from {senderName}"
      (only if user is NOT currently in that conversation — check Redis)

API:
  GET    /api/notifications?unread=true&page=0&size=20
  PATCH  /api/notifications/{id}/read
  PATCH  /api/notifications/read-all
  GET    /api/notifications/preferences
  PUT    /api/notifications/preferences
  WS     /ws/notifications

application.yml: port 8086, postgres-notifications:5432/unikly_notifications,
  kafka:9092, spring.data.redis.host=redis

Create Dockerfile. Add to docker-compose.yml (depends_on: postgres-notifications,
kafka, redis). Update settings.gradle. Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(notification-service): implement hybrid notification delivery with WebSocket, Redis presence, and Kafka event consumers"
```

### 📝 CHANGELOG

> **Added:** Notification Service — WebSocket STOMP real-time delivery, Redis user presence tracking (TTL-based), FCM push placeholder, polling fallback API, notification preferences, Kafka consumers for all domain events (proposals, payments, matching, messages), idempotent processing

---

## Step 3.3 — Angular: Notification Bell & Real-Time

### 📋 PROMPT

```text
Add real-time notifications to the Angular frontend.

Install: npm install @stomp/stompjs

1. core/services/websocket.service.ts
   - Connect to environment.wsUrl + '/notifications' using @stomp/stompjs Client
   - connectHeaders: { Authorization: 'Bearer ' + token }
     (or pass token as ?token= query param, whichever the backend expects)
   - Subscribe to /user/queue/notifications
   - Expose notifications$ = new Subject<any>() that emits each incoming notification
   - Auto-reconnect: reconnectDelay = 2000, exponential backoff up to 30000ms
   - On connect: log "WebSocket connected"
   - On disconnect: start polling fallback (every 30s GET /api/notifications?unread=true)
   - Provide activate() and deactivate() methods

2. core/services/notification.service.ts
   - Injectable, provided in root
   - unreadCount = signal<number>(0)
   - notifications = signal<Notification[]>([])
   - On init: call getNotifications() to load initial unread count
   - Subscribe to websocket.notifications$: prepend to notifications(), increment unreadCount
   - Methods:
     getNotifications(page, size, unreadOnly): GET /api/notifications
     markAsRead(id): PATCH /api/notifications/{id}/read → decrement unreadCount
     markAllRead(): PATCH /api/notifications/read-all → set unreadCount to 0
     getPreferences(): GET /api/notifications/preferences
     updatePreferences(prefs): PUT /api/notifications/preferences

3. shared/components/notification-bell/notification-bell.component.ts (standalone)
   - Inject NotificationService
   - MatIconButton with 'notifications' icon
   - MatBadge showing unreadCount() (hidden when 0)
   - Click: open MatMenu (overlay panel) anchored to the bell icon
   - Panel content:
     Header: "Notifications" + "Mark all read" text button
     List of recent 10 notifications:
       Each: icon (based on type), title (bold), body (truncated), timeAgo pipe
       Click on notification: markAsRead(id), navigate to actionUrl, close menu
     Footer: "View all" link → navigate to /notifications
   - Animate badge on new notification (subtle pulse CSS animation)

4. features/notifications/notification-list.component.ts (standalone)
   - Full page: /notifications
   - MatButtonToggle: "All" / "Unread"
   - List with pagination (MatPaginator)
   - Each notification: type icon, title, body, time ago, read/unread visual indicator
   - Click: mark as read, navigate to actionUrl
   - Settings section: notification preferences toggles (email, push, real-time)

5. Update main-layout.component.ts:
   - Add <app-notification-bell> in the toolbar, next to user menu

Update routes (add /notifications). Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(frontend): add real-time notification bell with WebSocket, polling fallback, and notification management"
```

### 📝 CHANGELOG

> **Added:** WebSocket STOMP connection with auto-reconnect and polling fallback, notification bell with live unread badge and dropdown panel, full notification page with preferences management

---

# Phase 4: Payments & Messaging

> **Goal:** Full Stripe escrow payment flow and real-time chat working.

---

## Step 4.1 — Payment Service: Stripe & Escrow

### 📋 PROMPT

```text
Create backend/payment-service/ as a Spring Boot 4.0.3 Gradle submodule.

Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-kafka,
com.stripe:stripe-java:28.2.0 (or latest), spring-boot-starter-actuator,
resilience4j-spring-boot3, micrometer-registry-prometheus

Package: com.unikly.paymentservice.{domain, application, infrastructure, api}

⚠️ This is a FINANCIAL service. Correctness > speed. Double-check all state transitions.

DOMAIN:

PaymentStatus enum: PENDING, FUNDED, RELEASED, COMPLETED, FAILED, REFUNDED, DISPUTED
Valid transitions:
  PENDING → FUNDED, PENDING → FAILED
  FUNDED → RELEASED, FUNDED → REFUNDED, FUNDED → DISPUTED
  RELEASED → COMPLETED
  DISPUTED → REFUNDED, DISPUTED → RELEASED

Payment entity:
  - id: UUID PK
  - jobId: UUID (indexed)
  - clientId: UUID
  - freelancerId: UUID
  - amount: BigDecimal(12,2) @Positive
  - currency: String(3)
  - status: PaymentStatus (default PENDING)
  - stripePaymentIntentId: String (unique)
  - stripeTransferId: String (nullable)
  - idempotencyKey: String (unique) — generated by client, prevents duplicate charges
  - createdAt, updatedAt: Instant
  - version: int @Version

LedgerEntry entity:
  - id: UUID PK
  - paymentId: UUID (FK)
  - entryType: LedgerEntryType enum (ESCROW_FUND, ESCROW_RELEASE, REFUND, FEE_DEDUCTION)
  - amount: BigDecimal(12,2)
  - currency: String(3)
  - description: String
  - createdAt: Instant

WebhookEvent entity (idempotency for Stripe webhooks):
  - id: String PK (= Stripe event ID, e.g., "evt_1234...")
  - eventType: String
  - processedAt: Instant

APPLICATION:

PaymentService:

  createPaymentIntent(jobId, clientId, freelancerId, amount, currency, idempotencyKey):
    1. Check idempotencyKey doesn't already exist → 409 if duplicate
    2. Call Stripe: PaymentIntent.create({ amount (in cents), currency, metadata: {jobId} })
    3. Save Payment entity: status=PENDING, stripePaymentIntentId=pi.getId()
    4. Publish PaymentInitiatedEvent via outbox
    5. Return { paymentId, clientSecret: pi.getClientSecret() }

  handleWebhook(payload, sigHeader):
    1. Verify signature: Webhook.constructEvent(payload, sigHeader, webhookSecret)
    2. Extract Stripe event ID. Check WebhookEvent table — skip if exists.
    3. If event.type == "payment_intent.succeeded":
       Find Payment by stripePaymentIntentId
       Update status → FUNDED
       Create LedgerEntry(ESCROW_FUND)
       Save WebhookEvent
       Publish PaymentCompletedEvent via outbox
    4. If event.type == "payment_intent.payment_failed":
       Update status → FAILED. Save WebhookEvent.

  releaseEscrow(paymentId, clientId):
    1. Find payment, verify clientId is owner, verify status == FUNDED
    2. Call Stripe Transfer.create({ amount, currency, destination: freelancerStripeAccountId })
       (For MVP: just update status without real Stripe transfer — add TODO)
    3. Update status → RELEASED, then → COMPLETED
    4. Create LedgerEntry(ESCROW_RELEASE)
    5. Publish EscrowReleasedEvent via outbox

  requestRefund(paymentId, clientId):
    1. Verify status == FUNDED (can only refund before release)
    2. Call Stripe Refund.create({ payment_intent: stripePaymentIntentId })
    3. Update status → REFUNDED
    4. Create LedgerEntry(REFUND)

INFRASTRUCTURE:

StripeClient: wraps all Stripe SDK calls. Each method annotated with:
  @CircuitBreaker(name = "stripe", fallbackMethod = "stripeFallback")
  @Retry(name = "stripe")
  @TimeLimiter(name = "stripe")
  Fallback: throw PaymentProviderUnavailableException("Stripe temporarily unavailable")

Resilience4j config:
  resilience4j.circuitbreaker.instances.stripe:
    failureRateThreshold: 50
    waitDurationInOpenState: 30000
    slidingWindowSize: 10
  resilience4j.retry.instances.stripe:
    maxAttempts: 3
    waitDuration: 1000
    exponentialBackoffMultiplier: 2

Kafka producer: PaymentCompletedEvent, EscrowReleasedEvent → "payment.events"

API:
  POST   /api/payments                  → create payment intent, return clientSecret
  GET    /api/payments?jobId={uuid}     → get payment status
  POST   /api/payments/{id}/release     → release escrow (CLIENT only)
  POST   /api/payments/{id}/refund      → refund (FUNDED only)
  POST   /webhooks/stripe               → Stripe webhook (NO JWT AUTH, signature only)

⚠️ The /webhooks/stripe endpoint must NOT require JWT authentication.
   Security is via Stripe signature verification ONLY.

application.yml: port 8083, postgres-payments:5432/unikly_payments, kafka:9092,
  stripe.secret-key=${STRIPE_SECRET_KEY}, stripe.webhook-secret=${STRIPE_WEBHOOK_SECRET}

Create Dockerfile. Add to docker-compose.yml. Update settings.gradle.
Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(payment-service): implement escrow payment flow with Stripe integration, webhook processing, ledger, and event publishing"
```

### 📝 CHANGELOG

> **Added:** Payment Service — Stripe PaymentIntent creation, webhook processing with HMAC signature verification, escrow state machine (PENDING→FUNDED→RELEASED→COMPLETED), financial ledger entries, idempotent webhook processing via WebhookEvent table, PaymentCompletedEvent/EscrowReleasedEvent via outbox, Resilience4j circuit breaker + retry + timeout on all Stripe calls

---

## Step 4.2 — Angular: Payment UI with Stripe.js

### 📋 PROMPT

```text
Add payment functionality to the Angular frontend.

Install: npm install @stripe/stripe-js

1. core/services/payment.service.ts
   - loadStripe(): lazy-load Stripe.js with environment.stripePublishableKey
   - createPaymentIntent(jobId, amount, currency): POST /api/payments
   - getPaymentStatus(jobId): GET /api/payments?jobId={jobId}
   - releaseEscrow(paymentId): POST /api/payments/{id}/release
   - requestRefund(paymentId): POST /api/payments/{id}/refund

2. features/payments/payment-dialog.component.ts (standalone, MatDialog)
   - Input: job (with id, title, budget, currency, freelancerId)
   - Step 1: Payment summary — "Fund escrow for: {job.title}, Amount: {budget} {currency}"
   - Step 2: Call createPaymentIntent → receive clientSecret
   - Step 3: Mount Stripe PaymentElement (card input) using stripe.elements({ clientSecret })
   - Step 4: "Pay Now" button → stripe.confirmPayment({ elements, confirmParams: { return_url } })
     Or use redirect: 'if_required' to handle SPA flow
   - On success: close dialog, show snackbar "Payment processing..."
   - On error: show error message from Stripe below the form
   - Loading state: disable button, show spinner during Stripe calls

3. features/payments/payment-status.component.ts (standalone)
   - Input: jobId
   - On init: getPaymentStatus(jobId) → display status
   - Status badges with colors:
     PENDING → yellow/amber
     FUNDED → green "Escrow Funded ✓"
     RELEASED → blue "Payment Released"
     COMPLETED → green with checkmark
     FAILED → red
     REFUNDED → gray
   - If status == FUNDED and current user is job client:
     "Release Payment" prominent button (green)
     "Request Refund" text button (secondary)
   - Release button opens confirmation dialog: "Release {amount} {currency} to the freelancer?"

4. Update job-detail.component.ts:
   - After proposal is accepted and no payment exists:
     Show prominent "Fund Escrow" button with amount
   - If payment exists: show <app-payment-status [jobId]="job.id">
   - If job COMPLETED and payment FUNDED: highlight "Release Payment" as primary action

Add to environment.ts: stripePublishableKey: 'pk_test_...' (placeholder)

Document in README: test cards — 4242424242424242 (success), 4000000000000002 (decline)

Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(frontend): add Stripe.js payment dialog, escrow status display, and payment management"
```

### 📝 CHANGELOG

> **Added:** Stripe.js PaymentElement in Material dialog, payment status badges, escrow release/refund buttons, integrated into job detail page, test card documentation

---

## Step 4.3 — Messaging Service

### 📋 PROMPT

```text
Create backend/messaging-service/ as a Spring Boot 4.0.3 Gradle submodule.

Package: com.unikly.messagingservice.{domain, application, infrastructure, api}

Dependencies: spring-boot-starter-websocket, spring-boot-starter-web,
spring-boot-starter-data-jpa, spring-kafka, spring-boot-starter-data-redis,
spring-boot-starter-actuator

DOMAIN:

Conversation entity:
  - id: UUID PK
  - jobId: UUID (nullable — conversations can exist without a job)
  - participantIds: List<UUID> (PostgreSQL UUID[] array)
  - createdAt: Instant
  - lastMessageAt: Instant (updated on each new message)

Message entity:
  - id: UUID PK
  - conversationId: UUID (FK, indexed)
  - senderId: UUID
  - content: String (TEXT)
  - contentType: MessageContentType enum (TEXT, FILE_LINK, SYSTEM)
  - readAt: Instant (nullable)
  - createdAt: Instant (indexed, for pagination)

APPLICATION:

ConversationService:
  - getOrCreateConversation(participantIds, jobId): find existing by participant set, or create
  - getUserConversations(userId, page): find where userId in participantIds, sort by lastMessageAt DESC
  - getById(conversationId, userId): verify userId is a participant, else 403

MessageService:
  - sendMessage(conversationId, senderId, content, contentType):
    1. Verify sender is participant
    2. Save message
    3. Update conversation.lastMessageAt
    4. Push to recipient via WebSocket (if online — check Redis)
    5. Publish MessageSentEvent via outbox (preview = first 100 chars)
  - getMessages(conversationId, userId, page, size): paginated, oldest first within page
  - markAsRead(messageId, userId): set readAt = now (only if recipient)

INFRASTRUCTURE:

WebSocket config:
  - Endpoint: /ws/messages (with SockJS)
  - STOMP messaging
  - On message delivery: send to /user/{recipientId}/queue/messages
  - Auth: extract JWT from connect query param, validate, set principal
  - On CONNECT: set Redis "msg-user:{userId}:online" = true, TTL 120s
  - On DISCONNECT: remove key
  - Typing indicator: client sends to /app/typing, server broadcasts to
    /user/{recipientId}/queue/typing (ephemeral, not persisted)

Kafka producer: MessageSentEvent → "messaging.events" via outbox

API:
  GET    /api/messages/conversations?page=0&size=20
  GET    /api/messages/conversations/{id}?page=0&size=50
  POST   /api/messages/conversations/{id}    (body: { content, contentType })
  PATCH  /api/messages/{messageId}/read
  WS     /ws/messages

application.yml: port 8085, postgres-messaging:5432/unikly_messaging, kafka:9092, redis

Create Dockerfile. Add to docker-compose.yml. Update settings.gradle.
Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(messaging-service): implement real-time messaging with WebSocket, conversation management, and Kafka events"
```

### 📝 CHANGELOG

> **Added:** Messaging Service — conversation CRUD with participant validation, message persistence, real-time WebSocket STOMP delivery, typing indicators, read receipts, MessageSentEvent via outbox, Redis connection tracking

---

## Step 4.4 — Angular: Chat Interface

### 📋 PROMPT

```text
Implement messaging UI in Angular frontend.

1. core/services/message-websocket.service.ts
   - STOMP connection to environment.wsUrl + '/messages'
   - Subscribe to /user/queue/messages → messages$ Subject
   - Subscribe to /user/queue/typing → typing$ Subject
   - sendTypingIndicator(conversationId, recipientId): publish to /app/typing
   - activate() / deactivate() lifecycle methods

2. features/messaging/services/messaging.service.ts
   - getConversations(page): GET /api/messages/conversations
   - getMessages(conversationId, page, size): GET /api/messages/conversations/{id}
   - sendMessage(conversationId, content): POST /api/messages/conversations/{id}
   - markAsRead(messageId): PATCH /api/messages/{messageId}/read

3. features/messaging/conversation-list.component.ts (standalone)
   - Route: /messages
   - List of conversations, each showing:
     Other participant: avatar + displayName (need to resolve from user service or cache)
     Last message preview (truncated to ~50 chars)
     Time ago (lastMessageAt)
     Unread indicator (bold text if last message not read by current user)
   - Sorted by lastMessageAt DESC
   - Empty state: "No conversations yet. Start one from a freelancer's profile!"
   - On new message via WebSocket: move conversation to top, update preview

4. features/messaging/conversation.component.ts (standalone)
   - Route: /messages/:id
   - Header: other user's avatar + name, "View Profile" link
   - Message area:
     Scrollable div with messages
     Sender messages: right-aligned, primary color background
     Recipient messages: left-aligned, light gray background
     Each: content, timestamp, read receipt (double-check icon if readAt exists)
   - Scroll to bottom on load and on new message
   - Scroll up to load older messages (intersection observer on top sentinel element,
     triggers getMessages with next page)
   - Input area at bottom:
     MatInput with "Type a message..." placeholder
     Send button (MatIconButton, 'send' icon)
     Send on Enter key, Shift+Enter for newline
   - Typing indicator:
     On keystroke: debounce 500ms, send typing indicator via WebSocket
     Display "User is typing..." animation when typing$ emits for this conversation
   - Real-time: new messages via WebSocket appear instantly, auto-scroll
   - On open: markAsRead for last unread message

5. Update main-layout sidebar: add "Messages" nav link with MatIcon 'chat'

6. Update public-profile.component.ts: "Send Message" button →
   Navigate to /messages after creating conversation (or opening existing one)

Update routes. Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(frontend): implement chat interface with real-time WebSocket messaging, typing indicators, and read receipts"
```

### 📝 CHANGELOG

> **Added:** Chat interface — conversation list with unread indicators, message thread with bubble layout, real-time delivery via WebSocket, typing indicators, read receipts, infinite scroll for history, send-on-Enter, "Send Message" from profiles

---

# Phase 5: Observability, Resilience & Polish

> **Goal:** Production-ready monitoring, error handling, documentation, and hardened resilience.

---

## Step 5.1 — Prometheus Metrics & Grafana Dashboards

### 📋 PROMPT

```text
Configure comprehensive observability across all services.

1. Verify ALL 7 Spring Boot services + gateway have these dependencies:
   - spring-boot-starter-actuator
   - micrometer-registry-prometheus
   And this config in application.yml:
     management.endpoints.web.exposure.include: health,prometheus,info,metrics
     management.metrics.tags.service: ${spring.application.name}

2. Add CUSTOM business metrics using Micrometer MeterRegistry:

   Job Service:
     - Counter "unikly_job_created_total" tag: status
     - Counter "unikly_proposal_submitted_total"
     - Timer "unikly_job_status_transition_seconds"

   Payment Service:
     - Counter "unikly_payment_completed_total" tag: currency
     - Counter "unikly_payment_failed_total"
     - Timer "unikly_stripe_api_call_seconds"

   Matching Service:
     - Counter "unikly_match_generated_total" tag: strategy
     - Timer "unikly_matching_duration_seconds"
     - Gauge "unikly_matching_queue_size"

3. Update infra/prometheus/prometheus.yml:
   Scrape configs for: gateway:8080, job-service:8081, user-service:8082,
   payment-service:8083, matching-service:8084, messaging-service:8085,
   notification-service:8086, search-service:8087
   Path: /actuator/prometheus, interval: 15s

4. Create Grafana dashboards in infra/grafana/dashboards/:
   - service-overview.json: per-service request rate, error rate (4xx+5xx),
     p50/p95/p99 response time, JVM heap usage, thread count
   - kafka-overview.json: consumer lag per group, messages produced/consumed per topic
   - business-metrics.json: jobs created over time, payments completed, matches generated
   - alerts.json: circuit breaker state per service, DLQ depth

5. Create infra/grafana/provisioning/datasources.yml → Prometheus at http://prometheus:9090
6. Create infra/grafana/provisioning/dashboards.yml → auto-load from /var/lib/grafana/dashboards/
7. Update docker-compose.monitoring.yml: mount dashboards + provisioning dirs into Grafana

Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(observability): add Prometheus metrics, Grafana dashboards, and custom business metrics across all services"
```

---

## Step 5.2 — Distributed Tracing

### 📋 PROMPT

```text
Add distributed tracing to ALL backend services.

1. Add dependencies to EVERY service build.gradle:
   - io.micrometer:micrometer-tracing-bridge-otel
   - io.opentelemetry:opentelemetry-exporter-otlp

2. application.yml for each service:
   management.tracing.sampling.probability: 1.0
   management.otlp.tracing.endpoint: http://tempo:4318/v1/traces

3. Kafka tracing: configure ObservationEnabled on Spring Kafka
   ProducerFactory and ConsumerFactory so traceId propagates through
   Kafka message headers automatically.

4. Structured JSON logging for ALL services:
   Add dependency: net.logstash.logback:logstash-logback-encoder:8.0
   Create src/main/resources/logback-spring.xml per service:
     Console appender with LogstashEncoder
     Fields: timestamp, level, logger, message, service_name, traceId, spanId,
     userId (from MDC — set in a filter that reads X-User-Id header)

5. Add Grafana Tempo to docker-compose.monitoring.yml:
   image: grafana/tempo:latest
   port: 4318:4318 (OTLP HTTP receiver)
   volume for tempo data

6. Add Tempo as datasource in Grafana provisioning.

Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(observability): add distributed tracing with OpenTelemetry, structured JSON logging, and traceId propagation across HTTP and Kafka"
```

---

## Step 5.3 — Resilience Patterns Audit

### 📋 PROMPT

```text
Audit and harden resilience patterns across ALL services.

1. Gateway circuit breakers:
   Every route must have a CircuitBreaker filter with id matching the service name.
   Fallback: return 503 JSON { "error": "Service temporarily unavailable", "service": "{name}" }

2. Payment Service (most critical):
   Verify Resilience4j on ALL Stripe calls:
     @CircuitBreaker(name="stripe") — failureRate 50%, wait 30s
     @Retry(name="stripe") — 3 attempts, 1s/2s/4s exponential
     @TimeLimiter(name="stripe") — 5s timeout
   Fallback: PaymentProviderUnavailableException → 503 response

3. Matching Service:
   Circuit breaker on AI service REST call (matching-ai:8090):
     failureRate 50%, wait 30s, slidingWindow 10
   Fallback: use RuleBasedMatchingEngine (log "AI unavailable, using rule-based")

4. Search Service:
   Circuit breaker on Elasticsearch calls.
   Fallback: return empty results with 503 and message "Search temporarily unavailable"

5. ALL Kafka consumers verification checklist:
   □ Idempotent processing (processed_events table)
   □ Max retry count configured (3-5)
   □ DLQ routing after final failure
   □ Error logging includes traceId and eventId
   □ Consumer group ID is unique per service

6. Add /actuator/circuitbreakers endpoint to each service for monitoring.
   Also add /actuator/retries and /actuator/ratelimiters where applicable.

Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "fix(resilience): audit and harden circuit breakers, retries, timeouts, and DLQ routing across all services"
```

---

## Step 5.4 — OpenAPI Documentation

### 📋 PROMPT

```text
Add OpenAPI/Swagger documentation to all backend services.

1. Add to ALL service build.gradle:
   org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0

2. application.yml per service:
   springdoc.api-docs.path: /api-docs
   springdoc.swagger-ui.path: /swagger-ui.html

3. Create @Configuration class in each service:
   @OpenAPIDefinition with @Info(title, version, description)
   Example for Job Service: title="Unikly Job Service API", version="1.0.0"

4. Annotate ALL controllers with:
   @Tag(name = "Jobs", description = "Job lifecycle management")
   @Operation(summary = "Create a new job", description = "...")
   @ApiResponse(responseCode = "201", description = "Job created")
   @ApiResponse(responseCode = "400", description = "Validation failed")
   @Parameter annotations on query params

5. Annotate DTOs with @Schema:
   @Schema(description = "Job creation request")
   Field-level: @Schema(description = "Job title", example = "Build Angular Dashboard", maxLength = 200)

6. Gateway API doc aggregation:
   Add springdoc-openapi-starter-webflux-api (for reactive gateway)
   Configure: springdoc.swagger-ui.urls listing each service's /api-docs endpoint
   Users visit http://localhost:8080/swagger-ui.html and can switch between services

Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "docs: add OpenAPI/Swagger documentation to all services with gateway aggregation"
```

---

## Step 5.5 — Error Handling & Validation Hardening

### 📋 PROMPT

```text
Harden error handling and input validation across the entire system.

1. In common module, create GlobalExceptionHandler (@ControllerAdvice):

   Handles:
   - MethodArgumentNotValidException → 400 with per-field errors
   - ConstraintViolationException → 400
   - EntityNotFoundException (custom) → 404
   - InvalidStatusTransitionException (custom) → 409 Conflict
   - AccessDeniedException → 403
   - OptimisticLockingFailureException → 409 "Resource modified by another request"
   - PaymentProviderUnavailableException → 503
   - Generic Exception → 500 (NEVER expose stack trace)

   Response body format (consistent across ALL services):
   {
     "status": 400,
     "error": "Validation Failed",
     "message": "One or more fields have invalid values",
     "traceId": "abc123...",
     "timestamp": "2026-03-17T10:30:00Z",
     "details": [
       { "field": "title", "message": "must not be blank" },
       { "field": "budget", "message": "must be positive" }
     ]
   }

2. Each service extends this handler by importing it. Add service-specific
   exceptions as needed (e.g., PaymentAlreadyProcessedException in Payment Service).

3. Input validation audit — verify ALL endpoints have:
   - Job title: @NotBlank @Size(max=200)
   - Budget: @NotNull @Positive @Digits(integer=10, fraction=2)
   - Skills: @NotEmpty, each @Size(max=50)
   - Pagination: page @Min(0), size @Min(1) @Max(100)
   - UUID path params: catch IllegalArgumentException for malformed UUIDs → 400
   - Cover letter: @Size(max=5000)
   - Review rating: @Min(1) @Max(5)
   - Display name: @NotBlank @Size(max=100)
   - Bio: @Size(max=5000)

4. Angular error interceptor update:
   Parse the structured error response. Display:
   - For 400: show first detail message or generic "Invalid input"
   - For 403: "You don't have permission for this action"
   - For 404: "Resource not found"
   - For 409: "This resource was modified. Please refresh and try again."
   - For 503: "Service temporarily unavailable. Please try again."
   - For 500: "Something went wrong. Please try again later." + show traceId

5. Request logging filter (apply to ALL services):
   Log at INFO level: method, path, userId (from X-User-Id), responseStatus, durationMs
   Example: "POST /api/jobs user=uuid-123 status=201 duration=145ms"

Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "fix: harden error handling with global exception handler, input validation, and structured error responses"
```

---

# Phase 6: AI Matching & Final Deployment

> **Goal:** AI-enhanced matching, production-ready config, tests, docs, and GitHub deployment.

---

## Step 6.1 — AI Matching Microservice (Python/FastAPI)

### 📋 PROMPT

```text
Create matching-ai/ as a standalone Python FastAPI microservice.

Structure:
matching-ai/
├── app/
│   ├── __init__.py
│   ├── main.py          # FastAPI app with lifespan
│   ├── models.py         # Pydantic models
│   ├── matching_engine.py # Embedding + cosine similarity
│   └── config.py         # Settings from env
├── requirements.txt
├── Dockerfile
└── README.md

requirements.txt:
  fastapi==0.115.6
  uvicorn[standard]==0.34.0
  sentence-transformers==3.4.1
  numpy==2.2.3
  pydantic==2.10.6

models.py (Pydantic v2 models):
  class FreelancerProfile(BaseModel):
    user_id: str
    bio: str
    skills: list[str]
    hourly_rate: float
    average_rating: float

  class MatchRequest(BaseModel):
    job_id: str
    job_description: str
    job_skills: list[str]
    freelancers: list[FreelancerProfile]

  class MatchScore(BaseModel):
    freelancer_id: str
    score: float  # 0.0-1.0
    matched_skills: list[str]

  class MatchResponse(BaseModel):
    job_id: str
    matches: list[MatchScore]

matching_engine.py:
  - On module load: load sentence-transformers model "all-MiniLM-L6-v2"
  - generate_embedding(text: str) -> np.ndarray
  - compute_matches(request: MatchRequest) -> MatchResponse:
    1. Create job text = f"{request.job_description} Skills: {', '.join(request.job_skills)}"
    2. Generate job_embedding
    3. For each freelancer:
       Create freelancer text = f"{freelancer.bio} Skills: {', '.join(freelancer.skills)}"
       Generate freelancer_embedding
       score = cosine_similarity(job_embedding, freelancer_embedding)
       matched_skills = intersection of job_skills and freelancer.skills
    4. Sort by score descending
    5. Return top 20

main.py:
  - FastAPI app with lifespan context manager (load model on startup)
  - POST /api/ai/match → accepts MatchRequest, returns MatchResponse
  - GET /health → {"status": "ok", "model": "all-MiniLM-L6-v2"}
  - Add CORS middleware (allow all origins for dev)
  - Request logging middleware

Dockerfile:
  FROM python:3.12-slim
  WORKDIR /app
  COPY requirements.txt .
  RUN pip install --no-cache-dir -r requirements.txt
  COPY app/ ./app/
  # Pre-download model at build time:
  RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('all-MiniLM-L6-v2')"
  EXPOSE 8090
  CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]

Add to docker-compose.yml:
  matching-ai:
    build: ../matching-ai
    ports: 8090:8090
    healthcheck: curl -f http://localhost:8090/health || exit 1
    deploy.resources.limits.memory: 1g

NOW update the Java Matching Service (backend/matching-service):
  - Create AiMatchingClient in infrastructure layer:
    @Component, uses RestClient/WebClient to call http://matching-ai:8090/api/ai/match
    Wrapped with @CircuitBreaker(name="ai-matching", fallbackMethod="aiFallback")
  - Update MatchingService.processJobForMatching():
    1. Try AI matching first (call AiMatchingClient)
    2. Also run rule-based matching
    3. HYBRID score = (ai_score * 0.50) + (rule_score * 0.50)
    4. If AI circuit is open: fallback to 100% rule-based (log warning)
  - Update MatchResult: when hybrid, set strategy = AI_EMBEDDING

Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "feat(matching-ai): add Python FastAPI AI matching service with sentence-transformers embeddings and hybrid scoring"
```

---

## Step 6.2 — Nginx & Production Docker Compose

### 📋 PROMPT

```text
Create production-ready deployment configuration.

1. Update infra/nginx/nginx.conf:
   worker_processes auto;
   events { worker_connections 1024; }
   http {
     include mime.types;
     gzip on;
     gzip_types text/html application/javascript text/css application/json;

     # Security headers
     add_header X-Content-Type-Options nosniff;
     add_header X-Frame-Options DENY;
     add_header X-XSS-Protection "1; mode=block";
     add_header Referrer-Policy strict-origin-when-cross-origin;

     server {
       listen 80;

       # Angular SPA
       location / {
         root /usr/share/nginx/html;
         try_files $uri $uri/ /index.html;
         # Cache static assets
         location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
           expires 1y;
           add_header Cache-Control "public, immutable";
         }
       }

       # API proxy
       location /api/ {
         proxy_pass http://gateway:8080/api/;
         proxy_set_header Host $host;
         proxy_set_header X-Real-IP $remote_addr;
         proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
         proxy_set_header X-Forwarded-Proto $scheme;
       }

       # WebSocket proxy
       location /ws/ {
         proxy_pass http://gateway:8080/ws/;
         proxy_http_version 1.1;
         proxy_set_header Upgrade $http_upgrade;
         proxy_set_header Connection "upgrade";
         proxy_set_header Host $host;
         proxy_read_timeout 86400;
       }

       # Webhooks (direct to payment service via gateway)
       location /webhooks/ {
         proxy_pass http://gateway:8080/webhooks/;
         proxy_set_header Host $host;
         proxy_set_header X-Real-IP $remote_addr;
       }
     }
   }

2. Create infra/docker-compose.prod.yml (override file):
   All services: restart: unless-stopped
   Resource limits:
     gateway: 512m
     Each backend service: 768m
     kafka: 1g
     elasticsearch: 1g
     each postgres: 256m
     matching-ai: 1g
     redis: 256m
   Logging: json-file, max-size=10m, max-file=3
   Only nginx exposes ports (80, 443)
   Remove all other port mappings
   Environment overrides: spring.jpa.hibernate.ddl-auto=validate

3. Create Makefile at project root:
   dev:          cd infra && docker compose up -d
   dev-logs:     cd infra && docker compose logs -f $(SERVICE)
   dev-monitor:  cd infra && docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
   prod:         cd infra && docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
   build:        ./build.sh
   down:         cd infra && docker compose down
   clean:        cd infra && docker compose down -v --rmi local
   test:         cd backend && ./gradlew test
   status:       cd infra && docker compose ps

4. Create build.sh (executable):
   #!/bin/bash
   set -e
   echo "=== Building backend ==="
   cd backend && ./gradlew clean bootJar && cd ..
   echo "=== Building frontend ==="
   cd frontend/unikly-app && npm ci && npx ng build --configuration production && cd ../..
   echo "=== Building Docker images ==="
   cd infra && docker compose build
   echo "=== Build complete ==="

Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "infra: add Nginx reverse proxy, production Docker Compose, Makefile, and build script"
```

---

## Step 6.3 — Integration Testing

### 📋 PROMPT

```text
Create integration tests for critical service flows.

1. Add to ALL service build.gradle (test dependencies):
   testImplementation 'org.testcontainers:testcontainers'
   testImplementation 'org.testcontainers:postgresql'
   testImplementation 'org.testcontainers:kafka'
   testImplementation 'org.springframework.boot:spring-boot-testcontainers'
   testImplementation 'org.springframework.kafka:spring-kafka-test'

2. Job Service tests (src/test/java/.../jobservice/):

   @SpringBootTest @Testcontainers
   class JobLifecycleIntegrationTest:
     @Container PostgreSQL + Kafka testcontainers
     @Autowired MockMvc

     testCreateJob_returnsCreated():
       POST /api/jobs with valid body (mock X-User-Id, X-User-Roles headers)
       Assert: 201, body has id and status=DRAFT

     testCreateJob_invalidTitle_returns400():
       POST /api/jobs with blank title
       Assert: 400, body.details[0].field == "title"

     testFullJobLifecycle():
       Create DRAFT → transition to OPEN → submit proposal (different user)
       → accept proposal → verify contract created → verify Kafka events published

     testInvalidStatusTransition_returns400():
       Create DRAFT → try transition to COMPLETED
       Assert: 400 or 409 with meaningful error message

3. Payment Service tests:

   @SpringBootTest @Testcontainers
   class PaymentWebhookIntegrationTest:
     Mock Stripe SDK (use Mockito or WireMock)

     testCreatePaymentIntent_success():
       POST /api/payments → verify Payment saved with PENDING, Stripe called

     testWebhookProcessing_updatesToFunded():
       Simulate Stripe webhook (mock signature verification)
       Assert: Payment status = FUNDED, LedgerEntry created, Kafka event published

     testWebhookIdempotency_processesOnce():
       Send same webhook event twice
       Assert: Second call returns 200 but no duplicate processing

4. Matching Service tests:

   @SpringBootTest @Testcontainers
   class MatchingEngineIntegrationTest:

     testRuleBasedMatching_producesScores():
       Insert 5 FreelancerSkillCache entries
       Call computeMatches with a job requiring 3 skills
       Assert: results sorted by score DESC, scores between 0-1

     testIdempotentConsumer_skipsDuplicate():
       Process JobCreatedEvent once → matches created
       Process same event again → no duplicate matches

5. Run all tests: ./gradlew test
   Ensure ALL pass before committing.

Update CHANGELOG.md.
```

### 📦 COMMIT

```bash
git add -A && git commit -m "test: add integration tests with Testcontainers for Job, Payment, and Matching services"
```

---

## Step 6.4 — Final README & Documentation

### 📋 PROMPT

```text
Create comprehensive project documentation.

1. Rewrite README.md (replace the initial placeholder):

   # Unikly — AI-Powered Freelancing Platform

   Brief description (2-3 sentences)

   ## Architecture
   Event-driven microservices with DDD. Brief explanation + ASCII diagram showing:
   [Angular] → [Nginx] → [Gateway] → [Services] → [Kafka] → [Consumers]

   ## Tech Stack
   Table: Technology | Version | Purpose (10-12 rows, key technologies only)

   ## Prerequisites
   List with version requirements

   ## Quick Start
   ```bash
   git clone ...
   cd unikly
   cp infra/.env.example infra/.env  # Edit with your Stripe keys
   make dev
   # Wait ~2 minutes for all services to start
   make status
   # Run Keycloak test user setup:
   cd infra && ./keycloak/setup-test-users.sh
   # Open http://localhost:4200
   ```

   ## Project Structure
   Tree diagram (top 2 levels)

   ## Services
   Table: Service | Port | Description (all 8 services)

   ## API Documentation
   After starting: http://localhost:8080/swagger-ui.html

   ## Testing
   ```bash
   make test                    # Run all backend tests
   cd frontend/unikly-app && ng test  # Frontend unit tests
   ```

   ## Monitoring
   - Grafana: http://localhost:3000 (admin/admin)
   - Prometheus: http://localhost:9090

   ## Test Credentials
   Table of 3 test users with roles

   ## Stripe Test Cards
   4242 4242 4242 4242 — Success
   4000 0000 0000 0002 — Declined

2. Create docs/ directory:
   - docs/ARCHITECTURE.md: DDD bounded contexts, event flow (Mermaid sequence diagrams),
     service interaction patterns, data ownership
   - docs/API.md: All endpoints grouped by service, with example request/response
   - docs/DEPLOYMENT.md: Dev, staging, production setup instructions
   - docs/TROUBLESHOOTING.md: Common issues (Kafka not starting, Keycloak redirect,
     Elasticsearch OOM, Stripe webhook testing with stripe-cli)

3. Finalize CHANGELOG.md:
   Move ALL [Unreleased] entries to [1.0.0] with today's date
   Add release summary at top of [1.0.0]:
   "Initial release of Unikly — a fully event-driven microservices freelancing platform
   with AI matching, Stripe escrow payments, real-time messaging, and comprehensive observability."

4. Add LICENSE file (MIT license with your name and 2026)

Update CHANGELOG.md (this is the final update before tagging).
```

### 📦 COMMIT

```bash
git add -A && git commit -m "docs: add comprehensive README, architecture docs, API reference, deployment guide, and finalize CHANGELOG for v1.0.0"
```

---

## Step 6.5 — Git Tag & Push to GitHub

### 📋 PROMPT

```text
Final steps to push Unikly to GitHub.

1. Verify the project:
   git log --oneline  # Review all commits are clean and sequential
   make test           # All tests pass
   make dev            # Everything starts
   make status         # All services healthy

2. Tag the release:
   git tag -a v1.0.0 -m "Unikly v1.0.0 — Initial release: AI-powered freelancing platform with event-driven microservices"

3. Create GitHub repository:
   gh repo create unikly --public --source=. --remote=origin --description="AI-powered freelancing platform built with Spring Boot, Angular, Kafka, and DDD"
   # OR manually:
   git remote add origin https://github.com/{your-username}/unikly.git

4. Push:
   git push -u origin main
   git push origin v1.0.0

5. Create GitHub Release:
   gh release create v1.0.0 \
     --title "Unikly v1.0.0" \
     --notes "Initial release — see CHANGELOG.md for full details.

   Highlights:
   - 7 Spring Boot microservices + API Gateway
   - Angular 19 frontend with Material Design
   - Apache Kafka 4.2 event-driven architecture
   - AI matching with sentence-transformers
   - Stripe escrow payment system
   - Real-time messaging and notifications
   - Full observability (Prometheus, Grafana, OpenTelemetry)
   - Docker Compose deployment"

6. Verify on GitHub:
   - README renders correctly
   - CHANGELOG.md is readable
   - v1.0.0 tag and release are visible
   - Repository description is set

🎉 DONE! Unikly is live.
```

---

# Appendix: Complete Commit Log

The expected `git log --oneline --reverse` after completing all steps:

| Step | Commit Message |
|------|---------------|
| 0.1 | `chore: initialize monorepo with Gradle multi-module, common module, and project skeleton` |
| 0.2 | `feat(common): add shared event classes, DTOs, outbox pattern, and security utilities` |
| 1.1 | `infra: add Docker Compose with PostgreSQL (7 instances), Kafka 4.2 KRaft, Keycloak 26.5, Elasticsearch, Redis, Prometheus, Grafana` |
| 1.2 | `infra(keycloak): add realm export with clients, roles, and test user setup script` |
| 1.3 | `feat(gateway): implement API Gateway with JWT validation, routing, rate limiting, and circuit breakers` |
| 2.1 | `feat(user-service): implement User Profile Service with CRUD, reviews, Kafka events, and outbox pattern` |
| 2.2 | `feat(job-service): implement Job Service with lifecycle state machine, proposals, contracts, and event publishing` |
| 2.3 | `feat(job-service): add Kafka consumers for PaymentCompletedEvent and EscrowReleasedEvent with idempotent processing` |
| 2.4 | `feat(search-service): implement Search Service with Elasticsearch indexing, full-text search, and Kafka event sync` |
| 2.5 | `feat(frontend): initialize Angular 19 with Keycloak auth, routing, interceptors, and layout components` |
| 2.6 | `feat(frontend): implement job list, create, and detail pages with proposal workflow` |
| 2.7 | `feat(frontend): implement profile management, public profiles, unified search, and shared components` |
| 3.1 | `feat(matching-service): implement rule-based matching engine with async processing, skill cache, and Kafka event flow` |
| 3.2 | `feat(notification-service): implement hybrid notification delivery with WebSocket, Redis presence, and Kafka event consumers` |
| 3.3 | `feat(frontend): add real-time notification bell with WebSocket, polling fallback, and notification management` |
| 4.1 | `feat(payment-service): implement escrow payment flow with Stripe integration, webhook processing, ledger, and event publishing` |
| 4.2 | `feat(frontend): add Stripe.js payment dialog, escrow status display, and payment management` |
| 4.3 | `feat(messaging-service): implement real-time messaging with WebSocket, conversation management, and Kafka events` |
| 4.4 | `feat(frontend): implement chat interface with real-time WebSocket messaging, typing indicators, and read receipts` |
| 5.1 | `feat(observability): add Prometheus metrics, Grafana dashboards, and custom business metrics across all services` |
| 5.2 | `feat(observability): add distributed tracing with OpenTelemetry, structured JSON logging, and traceId propagation across HTTP and Kafka` |
| 5.3 | `fix(resilience): audit and harden circuit breakers, retries, timeouts, and DLQ routing across all services` |
| 5.4 | `docs: add OpenAPI/Swagger documentation to all services with gateway aggregation` |
| 5.5 | `fix: harden error handling with global exception handler, input validation, and structured error responses` |
| 6.1 | `feat(matching-ai): add Python FastAPI AI matching service with sentence-transformers embeddings and hybrid scoring` |
| 6.2 | `infra: add Nginx reverse proxy, production Docker Compose, Makefile, and build script` |
| 6.3 | `test: add integration tests with Testcontainers for Job, Payment, and Matching services` |
| 6.4 | `docs: add comprehensive README, architecture docs, API reference, deployment guide, and finalize CHANGELOG for v1.0.0` |
| 6.5 | `tag: v1.0.0` |

---

*— End of Implementation Guide —*
