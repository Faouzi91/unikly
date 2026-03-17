# Unikly — AI-Powered Freelancing Platform

Unikly connects clients who need work done with freelancers who can deliver, powered by intelligent AI matching, secure escrow payments, real-time messaging, and a hybrid notification system.

## Technology Stack

| Layer          | Technology                          | Version       |
|----------------|-------------------------------------|---------------|
| Backend        | Spring Boot (Java 25)               | 4.0.3         |
| Frontend       | Angular                             | 21            |
| Database       | PostgreSQL                          | 18            |
| Messaging      | Apache Kafka (KRaft)                | 4.2.0         |
| Identity       | Keycloak                            | 26.5.5        |
| Search         | Elasticsearch                       | 9.0.0         |
| Cache          | Redis                               | 7.4           |
| AI Matching    | Python / FastAPI                    | 3.12+         |
| Observability  | Prometheus + Grafana                | latest        |
| Containerization | Docker + Docker Compose           | latest        |

## Architecture

Event-driven microservices with Domain-Driven Design (DDD):

- **API Gateway** — Spring Cloud Gateway with JWT validation, rate limiting, circuit breakers
- **User Service** — Profile management, skills, reviews
- **Job Service** — Job lifecycle state machine, proposals, contracts
- **Payment Service** — Stripe escrow integration, ledger
- **Matching Service** — Rule-based + AI-enhanced freelancer matching
- **Messaging Service** — Real-time WebSocket chat
- **Notification Service** — WebSocket, push, polling fallback
- **Search Service** — Elasticsearch-backed full-text search

## Project Structure

```
unikly/
├── backend/                    # Gradle multi-module (Java 25, Spring Boot 4.0.3)
│   ├── build.gradle.kts        # Root Gradle build with shared deps
│   ├── settings.gradle.kts     # Includes all submodules
│   ├── gradle.properties       # Java 25, Spring Boot 4.0.3
│   └── common/                 # Shared module: DTOs, event classes, outbox base
├── frontend/                   # Angular 21 workspace
├── matching-ai/                # Python FastAPI AI matching service
├── infra/
│   ├── docker/                 # Dockerfiles per service
│   ├── keycloak/               # Realm export JSON
│   ├── prometheus/             # prometheus.yml
│   ├── grafana/                # Provisioned dashboards
│   └── nginx/                  # nginx.conf
├── .gitignore
├── .env.example
├── CHANGELOG.md
└── README.md
```

## Prerequisites

- Git
- Docker Desktop (with Docker Compose v2)
- Java 25 (OpenJDK)
- Node.js 22 LTS
- Gradle 8.14+
- Angular CLI 21.x (`npm install -g @angular/cli`)
- A Stripe test account with API keys

## Getting Started

1. Clone the repository
2. Copy `.env.example` to `infra/.env` and fill in values
3. Start infrastructure: `cd infra && docker compose up -d`
4. Build backend: `cd backend && ./gradlew clean build`
5. Start services via Docker Compose or individually

## License

Proprietary — All rights reserved.
