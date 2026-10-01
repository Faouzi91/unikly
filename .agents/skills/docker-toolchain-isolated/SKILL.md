---
name: docker-toolchain-isolated
description: >-
  Workflows and best practices for running Java 25 and Node 24 toolchains in isolated Docker containers.
  Use when compiling, testing, packaging, or deploying Unikly services without relying on host toolchains.
---

# Docker Toolchain Isolation Skill

This skill documents how to build, test, and run Unikly in an isolated containerized environment, avoiding host toolchain version conflicts.

## Host vs. Target Toolchain Discrepancy

| Component | Host System | Required by Unikly | Isolation Strategy |
| :--- | :--- | :--- | :--- |
| **Java JDK** | OpenJDK 17 | **Eclipse Temurin 25** | Containerized Maven execution with host `~/.m2` cache mount |
| **Node.js** | Node v22 | **Node v24.21.0 Alpine** | Containerized Angular build via Docker multi-stage image |
| **Database** | None (PostgreSQL 18) | **PostgreSQL 18.6 Alpine** | Docker Compose managed container on port `55432` |

> [!CAUTION]
> **NEVER run `./mvnw test` or `npm run build` directly on the host machine.**
> Doing so will result in bytecode mismatch errors (class file version 69.0 vs 61.0) or Node dependency incompatibilities.

---

## Standard Runbooks

### 1. Running Backend Unit & Integration Tests

Execute backend Maven tests using the isolated test container while mounting the host `~/.m2` cache to avoid re-downloading dependencies:

```bash
docker run --rm \
  -v "${HOME}/.m2:/root/.m2" \
  -v "$(pwd)/backend:/workspace" \
  -w /workspace \
  unikly-api-test \
  mvn test
```

### 2. Testing the Angular Production Build

Validate that the Angular client compiles without TypeScript or template binding errors:

```bash
docker build -t unikly-web-test ./frontend
```

### 3. Rebuilding & Restarting Running Compose Services

When backend code, database migrations, or frontend code changes, rebuild only the affected service:

```bash
# Rebuild and restart backend API
docker compose up --build -d api

# Rebuild and restart frontend web container
docker compose up --build -d web

# Rebuild all services cleanly
docker compose up --build -d
```

### 4. Inspecting Container Logs & Health

Check real-time startup logs to confirm Spring Boot and Flyway initialization:

```bash
# View backend Spring Boot startup & migration logs
docker logs --tail 30 unikly-api-1

# View database logs
docker logs --tail 30 unikly-database-1

# View web nginx logs
docker logs --tail 30 unikly-web-1
```
