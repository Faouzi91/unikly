# Unikly — AI-Powered Freelancing Platform

Unikly is a fully event-driven microservices freelancing platform that connects clients with freelancers through intelligent AI matching, secure Stripe escrow payments, real-time messaging, and hybrid notifications.

Built with **Spring Boot 4.0.3** (Java 25) · **Angular 21** · **Apache Kafka 4.2** (KRaft) · **Keycloak 26.5.5** · **PostgreSQL 18** · **Elasticsearch 9.0** · **Docker**

> **Note:** The original SRS specified Angular 19.x and Java 21 LTS. The implementation has been upgraded to Angular 21 and Java 25.

---

## Architecture

Event-driven microservices with Domain-Driven Design (DDD) and **Hexagonal Architecture (Ports & Adapters)**. Each service logically separates the primary/secondary adapters from the core domain logic in `application` and `domain` packages. The frontend runs on a strict **Angular 21 Standalone Component Architecture**. Each service owns its database; all cross-service communication is asynchronous through Kafka topics. Events are published using the **Outbox Pattern** for transactional reliability, and all consumers are **idempotent** via a `processed_events` table.

```
                         ┌──────────────────┐
                         │  Angular 21 SPA  │
                         │  (Keycloak PKCE) │
                         └────────┬─────────┘
                                  │ HTTPS / WebSocket
                                  ▼
                         ┌──────────────────┐
                         │  Nginx (port 80) │
                         └────────┬─────────┘
                                  ▼
                 ┌─────────────────────────────────┐
                 │  API Gateway (Spring Cloud, 8080)│
                 │  JWT · Rate Limiting · Breakers  │
                 └──┬──────┬──────┬──────┬──────┬──┘
                    │      │      │      │      │
                    ▼      ▼      ▼      ▼      ▼
              job:8081  user:8082  pay:8083  match:8084  msg:8085
              search:8087  notif:8086  ai:8090
                    │      │      │      │      │
                    └──────┴──────┴──────┴──────┘
                                  │
                    ┌─────────────────────────┐
                    │   Kafka 4.2 KRaft Bus   │
                    │   job · user · payment  │
                    │   matching · messaging  │
                    └─────────────────────────┘
```

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Spring Boot | 4.0.3 (Java 25) | Backend microservices |
| Angular | 21 | Single-page frontend |
| PostgreSQL | 18 | Per-service relational data |
| Apache Kafka | 4.2.0 (KRaft) | Async event bus |
| Keycloak | 26.5.5 | Identity & OAuth2/OIDC |
| Elasticsearch | 9.0 | Full-text job & freelancer search |
| Redis | 7.4 | Rate limiting, WebSocket presence |
| Python FastAPI | 3.12 | AI semantic matching (sentence-transformers) |
| Stripe | — | Escrow payments, webhooks |
| Prometheus + Grafana | latest | Metrics & dashboards |
| OpenTelemetry + Tempo | latest | Distributed tracing |
| Docker + Compose | latest | Containerisation |

---

## Services

| Service | Port | Description |
|---|---|---|
| API Gateway | 8080 | JWT auth, rate limiting, routing, circuit breakers |
| Job Service | 8081 | Job CRUD, lifecycle state machine, proposals, contracts |
| User Service | 8082 | Profiles, skills, reviews |
| Payment Service | 8083 | Stripe escrow payments, webhook processing, ledger |
| Matching Service | 8084 | Rule-based + AI hybrid freelancer matching |
| Messaging Service | 8085 | WebSocket real-time chat, conversation management |
| Notification Service | 8086 | WebSocket notifications, preferences, Redis presence |
| Search Service | 8087 | Elasticsearch full-text search for jobs and freelancers |
| Matching AI | 8090 | Python FastAPI, sentence-transformers semantic matching |

**Infrastructure:**

| Service | Port | Purpose |
|---|---|---|
| Keycloak | 8180 | Identity provider, OAuth2/OIDC |
| Kafka | 9092 | Event bus (KRaft, no ZooKeeper) |
| Elasticsearch | 9200 | Search index |
| Redis | 6379 | Cache, rate limiting, presence |
| Prometheus | 9090 | Metrics scraping |
| Grafana | 3000 | Dashboards |

---

## Prerequisites

- **Docker Desktop** (with Docker Compose v2)
- **Java 25** (OpenJDK / Temurin)
- **Node.js 22 LTS** + Angular CLI 21 (`npm install -g @angular/cli`)
- **Gradle 9.4+** (wrapper included)
- A **Stripe test account** with API keys

---

## Quick Start

```bash
git clone https://github.com/<your-username>/unikly.git
cd unikly

# 1. Configure secrets
cp .env.example .env
# Edit .env — set STRIPE_SECRET_KEY, STRIPE_WEBHOOK_SECRET, and Keycloak passwords

# 2. Start all infrastructure + services
docker compose up -d

# 3. Wait ~2 minutes for all services to start, then verify
docker compose ps

# 4. Start the Angular dev server
cd frontend/unikly-app
npm install
ng serve --proxy-config proxy.conf.json

# 5. Open the application
# http://localhost:4200
```

> **With Makefile:**
> ```bash
> make dev       # Start everything
> make status    # Check service health
> make test      # Run all tests
> ```

---

## Project Structure

```
unikly/
├── backend/                        # Gradle multi-module (Java 25 · Spring Boot 4.0.3)
│   ├── common/                     # Shared events, DTOs, outbox pattern, security utils
│   ├── gateway/                    # API Gateway (Spring Cloud Gateway, port 8080)
│   ├── job-service/                # Job lifecycle, proposals, contracts (port 8081)
│   ├── user-service/               # User profiles, reviews (port 8082)
│   ├── payment-service/            # Stripe escrow, ledger (port 8083)
│   ├── matching-service/           # Freelancer matching engine (port 8084)
│   ├── messaging-service/          # Real-time chat (port 8085)
│   ├── notification-service/       # WebSocket + push notifications (port 8086)
│   └── search-service/             # Elasticsearch search (port 8087)
├── frontend/
│   └── unikly-app/                 # Angular 21 workspace (standalone components)
├── matching-ai/                    # Python FastAPI AI matching (port 8090)
├── infra/
│   ├── docker/                     # Dockerfiles per service
│   ├── docker-compose.yml          # Development stack
│   ├── docker-compose.prod.yml     # Production overrides
│   ├── docker-compose.monitoring.yml # Prometheus · Grafana · Tempo
│   ├── keycloak/                   # Realm export + test user setup
│   ├── nginx/                      # nginx.conf (SPA + API proxy)
│   ├── prometheus/                 # prometheus.yml
│   ├── grafana/                    # Provisioned dashboards + datasources
│   └── tempo/                      # Grafana Tempo tracing config
├── docs/                           # Architecture · API · Deployment · Troubleshooting
├── srs.md                          # Software Requirements Specification
├── plan.md                         # Project plan & feature status
├── Makefile                        # Developer shortcuts
├── build.sh                        # End-to-end build script
└── CHANGELOG.md
```

---

## Key Features

### ✅ Implemented

- **Authentication**: Keycloak-based login with PKCE, social OAuth (Google/GitHub), JWT refresh
- **User Profiles**: Full CRUD, skills, portfolio, reviews with auto-create on first login
- **Job Management**: Complete lifecycle state machine (DRAFT → OPEN → IN_REVIEW → IN_PROGRESS → COMPLETED → CLOSED)
- **Proposals**: Submit, accept, reject with event-driven notifications
- **Payments**: Stripe escrow (create intent, release, refund) with idempotent webhook processing
- **AI Matching**: Hybrid rule-based + sentence-transformers semantic matching
- **Messaging**: Real-time WebSocket chat with read receipts and typing indicators
- **Notifications**: WebSocket push + paginated list, mark read
- **Search**: Elasticsearch full-text search for jobs and freelancers with autocomplete
- **Observability**: Prometheus metrics, Grafana dashboards, OpenTelemetry distributed tracing
- **Resilience**: Circuit breakers, rate limiting, DLQ, idempotent consumers, outbox pattern
- **Phase 1 (Foundation):** ✅ Completed (Services, API Gateway, Auth, AI Matching)
- **Phase 1b (Architecture Standardization):** ✅ Completed (Kafka, Spring Modulith, Hexagonal)
- **Phase 2 (Core Platform Functionality - File Uploads, Payments, Job Completion):** ✅ Completed
- **Phase 3 (Polishing & Production Readiness):** 🚧 Up Next

### 🚧 In Progress

- Stripe.js payment element UI in frontend
- 401 silent token refresh in auth interceptor
- Admin dashboard content
- Notification bell real-time updates
- Review submission UI

---

## API Documentation

After starting the stack, the aggregated Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

Select any service from the spec dropdown. For per-service endpoint documentation, see [docs/API.md](docs/API.md).

---

## Testing

```bash
# All backend integration tests (Testcontainers — requires Docker)
make test
# or: cd backend && ./gradlew test

# Frontend unit tests
cd frontend/unikly-app && ng test

# Individual service
cd backend && ./gradlew :job-service:test
```

---

## Monitoring

| Tool | URL | Credentials |
|---|---|---|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |
| Grafana Tempo | http://localhost:3200 | — |

Four pre-provisioned dashboards: **Service Overview**, **Kafka Overview**, **Business Metrics**, **Alerts**.

---

## Test Credentials

| User | Email | Password | Role |
|---|---|---|---|
| Client | testclient@unikly.com | Test1234! | ROLE_CLIENT |
| Freelancer | testfreelancer@unikly.com | Test1234! | ROLE_FREELANCER |
| Admin | admin@unikly.com | Admin1234! | ROLE_ADMIN |

> **Note:** Test users are seeded in Keycloak. Profiles are auto-created on first login (no manual setup needed).

---

## Stripe Test Cards

| Card Number | Scenario |
|---|---|
| 4242 4242 4242 4242 | Payment succeeds |
| 4000 0000 0000 0002 | Payment declined |
| 4000 0025 0000 3155 | 3D Secure required |

Use any future expiry date (e.g. 12/29) and any 3-digit CVC.

---

## Documentation

| Document | Description |
|---|---|
| [srs.md](srs.md) | Software Requirements Specification |
| [plan.md](plan.md) | Project plan & feature status matrix |
| [ARCHITECTURE.md](ARCHITECTURE.md) | DDD bounded contexts, event flows, diagrams |
| [docs/API.md](docs/API.md) | All REST endpoints with request/response examples |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | Dev, staging, and production setup |
| [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Common issues and solutions |
| [CHANGELOG.md](CHANGELOG.md) | Release history |
| [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md) | Production hardening notes |

---

## License

MIT — see [LICENSE](LICENSE).
