# Unikly — AI-Powered Freelancing Platform

Unikly is a fully event-driven microservices freelancing platform that connects clients with freelancers through intelligent AI matching, secure Stripe escrow payments, real-time messaging, and hybrid notifications. Built on Spring Boot 4.0, Angular 21, Apache Kafka 4.2, and PostgreSQL 18.

---

## Architecture

Event-driven microservices with Domain-Driven Design (DDD). Each service owns its database; all cross-service communication is asynchronous through Kafka topics.

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Browser / Mobile                                                         │
│  Angular 21 · Keycloak PKCE · Angular Material + Tailwind               │
└──────────────────────┬───────────────────────────────────────────────────┘
                       │ HTTPS / WebSocket
                       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  Nginx (port 80)  — Static SPA hosting + reverse proxy                  │
└──────────────────────┬───────────────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  API Gateway (port 8080) — Spring Cloud Gateway                          │
│  JWT validation · Rate limiting (Redis) · Circuit breakers               │
│  X-User-Id / X-User-Roles header relay                                   │
└──────┬────────┬──────────┬───────────┬──────────┬────────────────────────┘
       │        │          │           │          │
       ▼        ▼          ▼           ▼          ▼
  job:8081  user:8082  pay:8083  match:8084  msg:8085
  search:8087  notif:8086
       │        │          │           │          │
       └────────┴──────────┴───────────┴──────────┘
                           │
                           ▼
            ┌──────────────────────────┐
            │  Apache Kafka 4.2 KRaft  │
            │  job.events              │
            │  payment.events          │
            │  matching.events         │
            │  messaging.events        │
            │  user.events             │
            │  *.dlq (dead-letter)     │
            └──────────────────────────┘
                           │
              Consumers: search-service, notification-service,
                         matching-service, job-service
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
| Prometheus + Grafana | latest | Metrics & dashboards |
| OpenTelemetry + Tempo | latest | Distributed tracing |
| Docker + Compose | latest | Containerisation |

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

# Configure secrets
cp infra/.env.example infra/.env
# Edit infra/.env — set STRIPE_SECRET_KEY, STRIPE_WEBHOOK_SECRET, and Keycloak passwords

# Start everything
make dev

# Wait ~2 minutes for all services to start
make status

# Create test users in Keycloak
cd infra && ./keycloak/setup-test-users.sh

# Open the application
open http://localhost:4200
```

> **Alternative (without Makefile):**
> ```bash
> cd infra && docker compose up -d
> cd ../backend && ./gradlew bootJar
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
│   └── unikly-app/                 # Angular 21 workspace
├── matching-ai/                    # Python FastAPI AI matching (port 8090)
├── infra/
│   ├── docker/                     # Dockerfiles per service
│   ├── docker-compose.yml          # Development stack
│   ├── docker-compose.prod.yml     # Production overrides
│   ├── docker-compose.monitoring.yml # Prometheus · Grafana · Tempo
│   ├── keycloak/                   # Realm export + test user setup script
│   ├── nginx/                      # nginx.conf (SPA + API proxy)
│   ├── prometheus/                 # prometheus.yml
│   ├── grafana/                    # Provisioned dashboards + datasources
│   └── tempo/                      # Grafana Tempo tracing config
├── docs/                           # Architecture · API · Deployment · Troubleshooting
├── Makefile                        # Developer shortcuts
├── build.sh                        # End-to-end build script
└── CHANGELOG.md
```

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
| Keycloak | 8180 | Identity provider, OAuth2/OIDC |
| Kafka | 9092 | Event bus (KRaft, no ZooKeeper) |
| Elasticsearch | 9200 | Search index |
| Redis | 6379 | Cache, rate limiting, presence |
| Prometheus | 9090 | Metrics scraping |
| Grafana | 3000 | Dashboards |

---

## API Documentation

After starting the stack, the aggregated Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

Select any service from the spec dropdown. For per-service docs, see [docs/API.md](docs/API.md).

---

## Testing

```bash
# All backend integration tests (Testcontainers — requires Docker)
make test

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
| Client | client1@test.com | Password1! | ROLE_CLIENT |
| Freelancer | freelancer1@test.com | Password1! | ROLE_FREELANCER |
| Admin | admin@test.com | Password1! | ROLE_ADMIN |

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

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — DDD bounded contexts, event flows, service interactions
- [docs/API.md](docs/API.md) — All REST endpoints with example requests/responses
- [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) — Dev, staging, and production setup
- [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) — Common issues and solutions

---

## License

MIT — see [LICENSE](LICENSE).
