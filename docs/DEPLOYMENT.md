# Unikly — Deployment Guide

## Table of Contents

1. [Local Development](#local-development)
2. [Running Tests](#running-tests)
3. [Building for Production](#building-for-production)
4. [Production Docker Compose](#production-docker-compose)
5. [Environment Variables Reference](#environment-variables-reference)
6. [Keycloak Setup](#keycloak-setup)
7. [Stripe Webhook Setup](#stripe-webhook-setup)
8. [Staging / Cloud Deployment](#staging--cloud-deployment)

---

## Local Development

### Prerequisites

| Tool | Min Version | Install |
|---|---|---|
| Docker Desktop | 4.x | <https://docs.docker.com/get-docker/> |
| Java | 25 | `sdk install java 25-tem` |
| Node.js | 22 LTS | <https://nodejs.org> |
| Angular CLI | 21.x | `npm install -g @angular/cli` |
| Gradle | 9.4 (wrapper) | included in repo |

### Step-by-step

```bash
# 1. Clone
git clone https://github.com/<you>/unikly.git
cd unikly

# 2. Configure environment
cp infra/.env.example infra/.env
# Edit infra/.env — see Environment Variables section below

# 3. Start the full stack
make dev
# Equivalent to: cd infra && docker compose up -d

# 4. Wait for all containers to be healthy (~2 min)
make status

# 5. Import Keycloak test users
cd infra && ./keycloak/setup-test-users.sh

# 6. Open the app
open http://localhost:4200
```

### Makefile Targets

| Target | Action |
|---|---|
| `make dev` | Start all Docker Compose services |
| `make dev-logs` | Tail logs from all services |
| `make dev-monitor` | Also start monitoring stack (Prometheus, Grafana, Tempo) |
| `make build` | Run full build: Gradle bootJar + Angular prod build + Docker build |
| `make test` | Run all backend integration tests |
| `make down` | Stop and remove containers |
| `make clean` | Stop + remove containers, volumes, and built JARs |
| `make status` | Show health of all running containers |

### Frontend Development Server

```bash
cd frontend/unikly-app
npm install
ng serve
# App available at http://localhost:4200
# Proxies /api/** and /ws/** to the gateway at localhost:8080
```

### Running a Single Backend Service Locally

```bash
cd backend
./gradlew :job-service:bootRun
# Service starts on its configured port (e.g. 8081)
# Requires infra stack already running (postgres, kafka, keycloak, redis)
```

---

## Running Tests

```bash
# All backend integration tests (requires Docker — Testcontainers spins up PostgreSQL + Kafka)
make test
# Equivalent to: cd backend && ./gradlew test

# Single service
cd backend && ./gradlew :payment-service:test

# Frontend unit tests
cd frontend/unikly-app && ng test

# Frontend tests (headless CI mode)
cd frontend/unikly-app && ng test --watch=false --browsers=ChromeHeadless
```

---

## Building for Production

```bash
# Full end-to-end build
./build.sh
```

`build.sh` performs:

1. `cd backend && ./gradlew clean bootJar` — builds all service JARs
2. `cd frontend/unikly-app && ng build --configuration production` — builds Angular dist
3. `docker compose build` — builds all Docker images

---

## Production Docker Compose

```bash
# Start with production overrides (resource limits, restart policies, logging)
cd infra
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

Production overrides (`docker-compose.prod.yml`) add:

- `restart: unless-stopped` on all services
- Memory limits: gateway 512 MB, each backend service 768 MB, Kafka 1 GB, Elasticsearch 1 GB
- JSON-file log driver (`max-size: 10m, max-file: 3`) on all services
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` enforced on all Spring JPA services
- Nginx frontend on port 80 as the sole external entry point

### Nginx Entry Points (Production)

| Path | Destination |
|---|---|
| `/` | Angular SPA (static files) |
| `/api/**` | API Gateway (`http://gateway:8080`) |
| `/ws/**` | WebSocket proxy to Gateway |
| `/webhooks/**` | Stripe webhook endpoint (no auth) |

---

## Environment Variables Reference

Copy `infra/.env.example` to `infra/.env` and fill in your values.

### Required

| Variable | Example | Description |
|---|---|---|
| `STRIPE_SECRET_KEY` | `sk_test_xxx` | Stripe secret key (test or live) |
| `STRIPE_WEBHOOK_SECRET` | `whsec_xxx` | Stripe webhook signing secret |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin123` | Keycloak admin console password |
| `POSTGRES_PASSWORD` | `secret` | Shared Postgres password (all 7 instances) |

### Optional / Defaults

| Variable | Default | Description |
|---|---|---|
| `AI_SERVICE_URL` | `http://matching-ai:8090` | Matching AI microservice URL |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Kafka bootstrap address |
| `ELASTICSEARCH_URI` | `http://elasticsearch:9200` | Elasticsearch URI |
| `REDIS_HOST` | `redis` | Redis hostname |
| `KEYCLOAK_URL` | `http://keycloak:8180` | Internal Keycloak URL |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://tempo:4318/v1/traces` | OpenTelemetry trace endpoint |

---

## Keycloak Setup

Keycloak starts automatically with the realm pre-imported (`infra/keycloak/unikly-realm.json`). The import happens on the first boot via the `KC_IMPORT` environment variable.

### Admin Console

Open <http://localhost:8180/admin> and log in with `admin` / `<KEYCLOAK_ADMIN_PASSWORD>`.

### Realm Configuration

| Setting | Value |
|---|---|
| Realm | `unikly` |
| Frontend client | `unikly-frontend` (public, PKCE S256) |
| Backend client | `unikly-backend` (confidential, service account) |
| Roles | `ROLE_CLIENT`, `ROLE_FREELANCER`, `ROLE_ADMIN` |
| Access token TTL | 5 minutes |
| SSO session | 30 days |

### Test Users

Run the setup script once after first start:

```bash
cd infra && ./keycloak/setup-test-users.sh
```

Creates:

| Email | Password | Role |
|---|---|---|
| `client1@test.com` | `Password1!` | ROLE_CLIENT |
| `freelancer1@test.com` | `Password1!` | ROLE_FREELANCER |
| `admin@test.com` | `Password1!` | ROLE_ADMIN |

### Production Keycloak

For production, use an external Keycloak instance (or Keycloak Operator on Kubernetes). Update the `KEYCLOAK_URL` env variable and configure proper TLS on the frontend client.

---

## Stripe Webhook Setup

### Local Testing with Stripe CLI

```bash
# Install Stripe CLI
brew install stripe/stripe-cli/stripe

# Login
stripe login

# Forward webhooks to local payment-service (via gateway)
stripe listen --forward-to http://localhost:8080/webhooks/stripe

# The CLI will print a webhook secret — add it to infra/.env as STRIPE_WEBHOOK_SECRET
```

### Production Webhook

In the Stripe Dashboard:

1. Go to **Developers → Webhooks → Add endpoint**
2. URL: `https://your-domain.com/webhooks/stripe`
3. Events to listen to: `payment_intent.succeeded`, `payment_intent.payment_failed`
4. Copy the **Signing secret** to your production env as `STRIPE_WEBHOOK_SECRET`

---

## Staging / Cloud Deployment

Unikly is containerised and can run on any container platform. Below are notes for common targets.

### Docker Swarm

```bash
docker stack deploy -c infra/docker-compose.yml -c infra/docker-compose.prod.yml unikly
```

### Kubernetes (Helm)

A Helm chart is not included in v1.0.0 but can be generated from the Docker Compose definitions using [Kompose](https://kompose.io/):

```bash
cd infra && kompose convert -f docker-compose.yml -f docker-compose.prod.yml
```

Key Kubernetes considerations:

- Use `ConfigMap` + `Secret` for environment variables (replace `.env` file).
- Replace single-replica Kafka with a proper Kafka operator (Strimzi).
- Replace single PostgreSQL instances with managed databases (RDS, Cloud SQL).
- Use an Ingress controller instead of Nginx service.

### CI/CD

The project includes a `Makefile` with a `test` target suitable for CI:

```yaml
# Example GitHub Actions step
- name: Run backend tests
  run: make test
```

Tests use Testcontainers and require a Docker daemon in the CI environment. On GitHub Actions, `ubuntu-latest` runners have Docker available by default.
