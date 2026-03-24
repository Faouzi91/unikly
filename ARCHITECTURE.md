# Unikly — Architecture Diagram

> **Last updated:** Step 2.5 — Angular Frontend Setup & Auth

---

## High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTS                                        │
│                                                                             │
│   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                  │
│   │ Angular 21   │    │  Mobile App  │    │  External    │                  │
│   │ (port 4200)  │    │  (future)    │    │  Webhooks    │                  │
│   │ Keycloak PKCE│    │              │    │              │                  │
│   │ Mat+Tailwind │    │              │    │              │                  │
│   └──────┬───────┘    └──────┬───────┘    └──────┬───────┘                  │
│          │                   │                   │                          │
└──────────┼───────────────────┼───────────────────┼──────────────────────────┘
           │ HTTPS             │                   │
           ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        API GATEWAY (port 8080)                              │
│                     Spring Cloud Gateway (Reactive)                         │
│                                                                             │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐    │
│  │ JWT Validate│  │ Rate Limiter │  │ Circuit      │  │ Header Relay   │    │
│  │ (Keycloak)  │  │ (Redis)      │  │ Breaker      │  │ (X-User-Id,    │    │
│  │             │  │ 100 req/min  │  │(Resilience4j)│  │  X-User-Roles) │    │
│  └─────────────┘  └──────────────┘  └──────────────┘  └────────────────┘    │
│                                                                             │
│  Routes:  /api/jobs/**  → job-service:8081                                  │
│           /api/users/** → user-service:8082                                 │
│           /api/payments/** → payment-service:8083 (planned)                 │
│           /api/matching/** → matching-service:8084 (planned)                │
│           /api/messages/** → messaging-service:8085 (planned)               │
│           /api/notifications/** → notification-service:8086 (planned)       │
│           /api/search/** → search-service:8087                              │
│           /webhooks/**  → webhooks (no auth)                                │
└───────────┬────────────────────┬──────────────────────┬─────────────────────┘
            │                    │                      │
            ▼                    ▼                      ▼
┌───────────────────────┐  ┌───────────────────────┐  ┌───────────────────────┐
│     JOB SERVICE       │  │     USER SERVICE      │  │    SEARCH SERVICE     │
│     (port 8081)       │  │     (port 8082)       │  │     (port 8087)       │
├───────────────────────┤  ├───────────────────────┤  ├───────────────────────┤
│ Domain:               │  │ Domain:               │  │ Domain:               │
│ ├── Job               │  │ ├── UserProfile       │  │ ├── JobDocument       │
│ ├── Proposal          │  │ └── Review            │  │ ├── FreelancerDoc     │
│ ├── Contract          │  │                       │  │ └── ProcessedEventDoc │
│ └── ProcessedEvent    │  │ Application:          │  │                       │
│                       │  │ ├── UserProfileSvc    │  │ Application:          │
│ Application:          │  │ └── ReviewService     │  │ └── SearchService     │
│ ├── JobService        │  │                       │  │                       │
│ └── ProposalSvc       │  │ API Endpoints:        │  │ API Endpoints:        │
│                       │  │ ├── GET /api/users    │  │ ├── GET /search/jobs  │
│ Infrastructure:       │  │ ├── PUT /api/users    │  │ ├── GET /search/free  │
│ ├── PaymentConsumer   │  │ └── POST reviews      │  │ └── GET /suggestions  │
│ ├── KafkaConfig       │  │                       │  │                       │
│ └── OutboxPublisher   │  │ Publishes:            │  │ Consumes:             │
│                       │  │ └── UserProfileUpdated│  │ ├── JobEvents         │
│ State Machine:        │  │     → user.events     │  │ └── UserEvents        │
│ DRAFT → OPEN →        │  │                       │  │                       │
│ IN_PROGRESS →         │  │ DB: postgres-users    │  │ DB: Elasticsearch     │
│ COMPLETED → CLOSED    │  │     (port 5434)       │  │     (port 9200)       │
├───────────────────────┤  │                       │  │                       │
│ DB: postgres-jobs     │  │                       │  │                       │
│     (port 5433)       │  │                       │  │                       │
└───────────────────────┘  └───────────────────────┘  └───────────────────────┘

```

---

## Event Flow Diagram (4-Box Model)

```text
┌───────────────────────┐                    ┌───────────────────────┐
│      JOB SERVICE      │                    │     USER SERVICE      │
│   (Status & Outbox)   │                    │   (Profile & Outbox)  │
└───────────┬───────────┘                    └───────────┬───────────┘
            │                                            │
      publish (job.evt)                            publish (user.evt)
            │                                            │
            ▼                                            ▼
┌────────────────────────────────────────────────────────────────────┐
│                       KAFKA EVENT BUS (KRaft)                      │
│                                                                    │
│  job.events  ────► [Search, Match, Notif(P), Audit(P)]             │
│  user.events ────► [Search, Match, Audit(P)]                       │
│  payment.evt ────► [Job Svc, Notif(P), Audit(P)]                   │
│  match.event ────► [Notif(P), Audit(P)]                            │
└───────┬──────────────────────┬──────────────────────┬──────────────┘
        │                      │                      │
     consume                consume                consume
  (job/user.evt)         (job/user.evt)         (payment.evt)
        │                      │                      │
        ▼                      ▼                      ▼
┌───────────────────────┐  ┌───────────────────────┐  ┌───────────────────────┐
│    SEARCH SERVICE     │  │   MATCHING SERVICE    │  │     JOB SERVICE       │
│   (Indexing/Search)   │  │   (Scoring/Ranking)   │  │   (Payment Handler)   │
└───────────────────────┘  └───────────────────────┘  └───────────────────────┘

* (P) = Planned SRS Flow | Note: Job Service is both a producer and consumer.
```

---

## Kafka Topics

| Topic                | Partitions | Producers                   | Consumers (current) |
| -------------------- | ---------- | --------------------------- | ------------------- |
| `job.events`         | 6          | Job Service                 | Search Service      |
| `user.events`        | 4          | User Service                | Search Service      |
| `payment.events`     | 4          | _(Payment Svc — planned)_   | Job Service         |
| `matching.events`    | 4          | _(Matching Svc — planned)_  | —                   |
| `messaging.events`   | 6          | _(Messaging Svc — planned)_ | —                   |
| `notification.dlq`   | 2          | Error handler               | _(Manual review)_   |
| `payment.dlq`        | 2          | Error handler               | _(Manual review)_   |
| `job-service.dlq`    | 2          | Job Svc DLQ                 | _(Manual review)_   |
| `search-service.dlq` | 2          | Search Svc DLQ              | _(Manual review)_   |

---

## Event Types Published

| Event                     | Source       | Topic         | Trigger                      |
| ------------------------- | ------------ | ------------- | ---------------------------- |
| `JobCreatedEvent`         | Job Service  | `job.events`  | Job transitions DRAFT → OPEN |
| `JobStatusChangedEvent`   | Job Service  | `job.events`  | Any job status transition    |
| `ProposalSubmittedEvent`  | Job Service  | `job.events`  | Freelancer submits proposal  |
| `ProposalAcceptedEvent`   | Job Service  | `job.events`  | Client accepts proposal      |
| `UserProfileUpdatedEvent` | User Service | `user.events` | User updates their profile   |

---

## Event Types Consumed

| Event                     | Consumer       | Source Topic     | Action                                                          |
| ------------------------- | -------------- | ---------------- | --------------------------------------------------------------- |
| `PaymentCompletedEvent`   | Job Service    | `payment.events` | Transition job OPEN → IN_PROGRESS (if accepted proposal exists) |
| `EscrowReleasedEvent`     | Job Service    | `payment.events` | Log event, mark payment as settled                              |
| `JobCreatedEvent`         | Search Service | `job.events`     | Index new job document in Elasticsearch                         |
| `JobStatusChangedEvent`   | Search Service | `job.events`     | Update status; delete if CANCELLED/CLOSED                       |
| `UserProfileUpdatedEvent` | Search Service | `user.events`    | Re-index freelancer document (REST + event data)                |

---

## Infrastructure Services

```
┌─────────────────────────────────────────────────────────────┐
│                   INFRASTRUCTURE (Docker Compose)           │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │  Keycloak    │  │  Kafka 4.2   │  │  Redis 7.4   │       │
│  │  26.5.5      │  │  (KRaft)     │  │              │       │
│  │  port 8180   │  │  port 9092   │  │  port 6379   │       │
│  │              │  │              │  │              │       │
│  │  Realm:      │  │  No ZooKeeper│  │ Rate limiting│       │
│  │  "unikly"    │  │              │  │  (Gateway)   │       │
│  │  2 clients   │  │              │  │              │       │
│  │  3 roles     │  │              │  │              │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ Elasticsearch│  │  Prometheus  │  │  Grafana     │       │
│  │ 9.0          │  │  port 9090   │  │  port 3000   │       │
│  │ port 9200    │  │              │  │              │       │
│  │ (for Search) │  │  Scrapes     │  │  Dashboards  │       │
│  └──────────────┘  │  /actuator/  │  │              │       │
│                    │  prometheus  │  │              │       │
│                    └──────────────┘  └──────────────┘       │
│                                                             │
│  DATABASES (PostgreSQL 18, one per service):                │
│  ┌────────────┬────────────┬────────────┬────────────┐      │
│  │ jobs       │ users      │ payments   │ matching   │      │
│  │ :5433      │ :5434      │ :5435      │ :5436      │      │
│  ├────────────┼────────────┼────────────┼────────────┤      │
│  │ messaging  │ notif.     │ keycloak   │            │      │
│  │ :5437      │ :5438      │ :5439      │            │      │
│  └────────────┴────────────┴────────────┴────────────┘      │
└─────────────────────────────────────────────────────────────

---

## Idempotent Consumer Pattern (Step 2.3)

```

Kafka Message Arrives
│
▼
┌─────────────────┐
│ Deserialize to │
│ BaseEvent │
│ (Jackson poly- │
│ morphic) │
└────────┬────────┘
│
▼
┌─────────────────┐ ┌─────────────────┐
│ Check │────►│ processed_events│
│ eventId exists? │ │ table (PK check)│
│ │◄────│ │
└────────┬────────┘ └─────────────────┘
│
┌─────┴─────┐
│ │
EXISTS NOT EXISTS
│ │
▼ ▼
┌──────┐ ┌───────────────┐
│ SKIP │ │ Process event │
│ +log │ │ + save eventId│
└──────┘ │ in SAME @Tx │
└───────────────┘
│
on failure (3x)
│
▼
┌──────────────┐
│ job-service │
│ .dlq topic │
└──────────────┘

```

---

## Security Flow

```

Client (Browser)
│
│ 1. Login via Keycloak (PKCE + S256)
▼
┌───────────┐
│ Keycloak │──── Issues JWT (access_token)
└───────────┘
│
│ 2. Request with Bearer token
▼
┌───────────┐
│ Gateway │──── Validates JWT signature + expiry
│ │──── Extracts: sub → X-User-Id
│ │──── roles → X-User-Roles
└───────────┘
│
│ 3. Forwards with headers
▼
┌───────────┐
│ Service │──── UserContext reads X-User-Id / X-User-Roles
│ │──── requireRole() for authorization
└───────────┘

```

---

## Implementation Progress

| Step | Service           | Status      |
|------|-------------------|-------------|
| 0.1  | Project Init      | Done        |
| 0.2  | Common Module     | Done        |
| 1.1  | Docker Compose    | Done        |
| 1.2  | Keycloak Realm    | Done        |
| 1.3  | API Gateway       | Done        |
| 2.1  | User Service      | Done        |
| 2.2  | Job Service       | Done        |
| 2.3  | Job Event Consume | Done        |
| 2.4  | Search Service    | Done        |
| 2.5  | Frontend Setup    | Done        |
| 3.1  | Payment Service   | Done        |
| 3.2  | Matching Service  | Done        |
| 3.3  | Messaging Service | Done        |
| 3.4  | Notification Svc  | Done        |
| 4.x  | Frontend (Angular)| _Planned_   |
| 5.x  | AI Matching       | Done        |
| 6.x  | Deploy & CI/CD    | _Planned_   |
```
