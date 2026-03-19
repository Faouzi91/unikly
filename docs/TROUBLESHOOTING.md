# Unikly — Troubleshooting

Common issues and how to resolve them.

---

## Table of Contents

1. [Kafka Not Starting](#kafka-not-starting)
2. [Keycloak Redirect Loop / CORS Error](#keycloak-redirect-loop--cors-error)
3. [Elasticsearch Out of Memory](#elasticsearch-out-of-memory)
4. [Stripe Webhook Testing Locally](#stripe-webhook-testing-locally)
5. [Service Fails to Start — Flyway Error](#service-fails-to-start--flyway-error)
6. [JWT Validation Fails (401)](#jwt-validation-fails-401)
7. [WebSocket Connection Drops](#websocket-connection-drops)
8. [Matching AI Not Scoring](#matching-ai-not-scoring)
9. [Circuit Breaker Open — 503 from Gateway](#circuit-breaker-open--503-from-gateway)
10. [Testcontainers Timeout in CI](#testcontainers-timeout-in-ci)
11. [General: Check Service Logs](#general-check-service-logs)

---

## Kafka Not Starting

**Symptom:** `kafka` container exits immediately or services log `Connection refused` to `kafka:9092`.

**Cause:** Kafka 4.2 runs in KRaft mode (no ZooKeeper). The most common issue is a stale `meta.properties` file in the Kafka data volume from a previous cluster ID.

**Fix:**

```bash
# Stop everything and remove Kafka volume
docker compose down -v
docker volume rm infra_unikly-kafka-data 2>/dev/null || true
docker compose up -d kafka
```

**Check Kafka health:**

```bash
docker compose logs kafka | grep "Kafka Server started"
```

**Verify topics were created:**

```bash
docker exec -it infra-kafka-1 \
  kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Expected topics: `job.events`, `payment.events`, `matching.events`, `messaging.events`, `user.events`, plus `*.dlq` topics.

---

## Keycloak Redirect Loop / CORS Error

**Symptom:** Browser shows redirect loop after login, or console shows `Access-Control-Allow-Origin` errors.

**Cause 1 — Realm not imported:** Keycloak started before the realm JSON was in place.

**Fix:**

```bash
docker compose restart keycloak
# Wait 30s then check:
docker compose logs keycloak | grep "Imported realm"
```

**Cause 2 — Wrong redirect URI:** The Keycloak `unikly-frontend` client only allows `http://localhost:4200/*`.

**Fix:** Open <http://localhost:8180/admin> → Clients → `unikly-frontend` → Valid redirect URIs. Add your URL if different.

**Cause 3 — Gateway CORS:** The gateway allows `http://localhost:4200` by default. If you changed the frontend port, update `spring.cloud.gateway.globalcors` in `gateway/src/main/resources/application.yml`.

---

## Elasticsearch Out of Memory

**Symptom:** `elasticsearch` container exits with `Exit 137` (OOM killed) or search endpoints return 503.

**Cause:** Elasticsearch requires at least 1 GB of heap. Docker Desktop on Mac/Windows defaults to a low memory limit.

**Fix 1 — Increase Docker memory:**

Docker Desktop → Settings → Resources → Memory → set to at least 6 GB.

**Fix 2 — Reduce Elasticsearch heap:**

In `infra/docker-compose.yml`, `elasticsearch` service:

```yaml
environment:
  - ES_JAVA_OPTS=-Xms512m -Xmx512m   # reduced from 1g
```

**Fix 3 — Disable `vm.max_map_count` warning (Linux only):**

```bash
sudo sysctl -w vm.max_map_count=262144
# Persistent: add to /etc/sysctl.conf
```

---

## Stripe Webhook Testing Locally

**Symptom:** Payments are created but never transition to FUNDED because Stripe can't reach your local service.

**Solution:** Use the Stripe CLI to forward events to your local gateway.

```bash
# Install Stripe CLI (macOS)
brew install stripe/stripe-cli/stripe

# Authenticate
stripe login

# Forward webhooks to local payment-service through the gateway
stripe listen --forward-to http://localhost:8080/webhooks/stripe
# Output: Ready! Your webhook signing secret is whsec_xxxxx

# Copy the signing secret into infra/.env:
# STRIPE_WEBHOOK_SECRET=whsec_xxxxx

# Restart payment-service to pick up the new secret
docker compose restart payment-service

# Trigger a test event manually
stripe trigger payment_intent.succeeded
```

**Verify webhook processing:**

```bash
docker compose logs payment-service | grep "Webhook event processed"
```

---

## Service Fails to Start — Flyway Error

**Symptom:** Service exits with `FlywayException: Found non-empty schema(s)` or `Schema validation: missing table`.

**Cause 1 — `ddl-auto=update` leftover:** Tables were created by Hibernate before Flyway migrations ran.

**Fix:** Drop the schema and let Flyway recreate it.

```bash
# Replace <db-port> with the service's PostgreSQL port (e.g. 5433 for jobs)
docker exec -it infra-postgres-jobs-1 psql -U unikly -d jobs -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
docker compose restart job-service
```

**Cause 2 — Migration checksum mismatch:** A migration file was modified after it was already applied.

**Fix:**

```bash
# Connect to the service DB and delete the bad row from flyway_schema_history
docker exec -it infra-postgres-jobs-1 psql -U unikly -d jobs \
  -c "DELETE FROM flyway_schema_history WHERE version='1';"
docker compose restart job-service
```

**Cause 3 — Wrong `CHAR` vs `VARCHAR` column type:** Hibernate schema validation rejects `bpchar` when the entity maps the column as `String`. Ensure all migration SQL uses `VARCHAR` not `CHAR`.

---

## JWT Validation Fails (401)

**Symptom:** All API calls return 401 even with a valid token.

**Cause 1 — Token expired:** Default access token TTL is 5 minutes.

**Fix:** Log out and log back in, or call `keycloak.updateToken(30)` to silently refresh.

**Cause 2 — Wrong issuer URI:** The gateway's `spring.security.oauth2.resourceserver.jwt.issuer-uri` must match Keycloak's issuer exactly (including `/realms/unikly`).

**Check:**

```bash
curl http://localhost:8180/realms/unikly/.well-known/openid-configuration | jq .issuer
# Should output: "http://localhost:8180/realms/unikly"
```

Compare with `gateway/src/main/resources/application.yml` → `spring.security.oauth2.resourceserver.jwt.issuer-uri`.

**Cause 3 — Keycloak not reachable from gateway container:** The gateway container resolves `keycloak` via Docker DNS. Check the container name matches the Compose service name.

---

## WebSocket Connection Drops

**Symptom:** Real-time notifications or messages stop updating; browser console shows WebSocket `CLOSE` frames.

**Cause 1 — Nginx proxy timeout:** Nginx closes idle WebSocket connections after 60s by default.

**Fix:** The included `nginx.conf` already sets `proxy_read_timeout 3600s` for `/ws/` paths. If you modified nginx.conf, ensure this is present.

**Cause 2 — JWT expired mid-session:** The STOMP connection sends the JWT on CONNECT only. If the token expires, the session is not automatically refreshed.

**Fix (frontend):** Call `keycloak.updateToken(60)` on a timer (every 4 minutes) and reconnect the STOMP client when the token is refreshed.

**Cause 3 — Redis presence key expired:** Notification presence keys have a 120s TTL refreshed every 60s by a `@Scheduled` task. If the notification-service is restarted, the scheduler resumes and presence is restored for active connections.

---

## Matching AI Not Scoring

**Symptom:** All match results show `strategy: RULE_BASED` with `score: 0.0` even after 30+ seconds.

**Cause 1 — matching-ai container still warming up:** The Python service downloads `all-MiniLM-L6-v2` at build time, but on first start it loads the model into memory (up to 60 seconds).

**Check:**

```bash
docker compose logs matching-ai | grep "Model loaded"
curl http://localhost:8090/health
```

**Cause 2 — Circuit breaker open:** If matching-ai failed previously, the circuit breaker in matching-service may be in OPEN state.

**Fix:**

```bash
# Check circuit breaker state
curl http://localhost:8084/actuator/circuitbreakers | jq .

# Reset by restarting matching-service
docker compose restart matching-service
```

**Cause 3 — No skill overlap:** Rule-based matching only considers freelancers with at least 1 matching skill. Ensure your test freelancer profile has skills that overlap with the job.

---

## Circuit Breaker Open — 503 from Gateway

**Symptom:** Some routes return `503 Service Unavailable` with `{"error": "Service unavailable", "service": "job-service"}`.

**Cause:** The Resilience4j circuit breaker tripped after the downstream service exceeded the failure threshold (50% of last 10 calls).

**Diagnose:**

```bash
# Check all circuit breaker states
curl http://localhost:8080/actuator/circuitbreakers | jq .

# Check the specific service
docker compose logs job-service | tail -50
```

**Fix:**

1. Resolve the underlying issue (DB connection, OOM, etc.)
2. The circuit will automatically transition to HALF_OPEN after 30 seconds and allow one probe request.
3. Or restart the service: `docker compose restart job-service`

---

## Testcontainers Timeout in CI

**Symptom:** Integration tests time out pulling Docker images in CI.

**Cause:** Testcontainers pulls `postgres:16` and `confluentinc/cp-kafka:7.6.0` on every run if not cached.

**Fix — pre-pull images in CI:**

```yaml
# GitHub Actions
- name: Pull Testcontainers images
  run: |
    docker pull postgres:16
    docker pull confluentinc/cp-kafka:7.6.0
```

**Fix — Ryuk timeout:** If Ryuk (Testcontainers resource reaper) times out, set:

```bash
export TESTCONTAINERS_RYUK_DISABLED=true  # CI environments without privileged Docker
```

Or in `src/test/resources/testcontainers.properties`:

```properties
ryuk.disabled=true
```

---

## General: Check Service Logs

```bash
# All services
make dev-logs

# Single service
docker compose logs -f job-service

# Since last restart
docker compose logs --since=5m payment-service

# Search for errors
docker compose logs matching-service 2>&1 | grep -i error

# Check container health
make status
# or:
docker compose ps
```

**Structured log fields** (JSON format, all services):

| Field | Description |
|---|---|
| `traceId` | OpenTelemetry trace ID — correlate across services in Grafana Tempo |
| `spanId` | Current span |
| `userId` | Authenticated user UUID (from MDC filter) |
| `level` | `INFO`, `WARN`, `ERROR` |
| `logger_name` | Java class name |
| `message` | Log message |
