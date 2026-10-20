---
name: docker-toolchain-isolated
description: >-
  Build and test Unikly with the project-pinned Java 25 and Node 24 toolchains in Docker. Use when compiling, testing, or running the local stack.
---

# Isolated Unikly toolchains

The pinned versions are .java-version (Java 25.0.4), .nvmrc (Node 24.21.0), Maven configuration, lockfiles, and Dockerfiles. Use Docker so commands do not silently run under a different host toolchain.

## Backend tests

Run Maven tests from the backend directory mounted into the pinned Maven/Temurin image:

    docker run --rm -v "$PWD/backend:/workspace" -w /workspace maven:3.9.16-eclipse-temurin-25 mvn -B -ntp test

This uses the project pom.xml and the image's Maven distribution. It does not require the local unikly-api-test image.

## Frontend production build

Build the frontend Dockerfile, which pins Node 24.21.0 and runs the Angular production build:

    docker build -t unikly-web-test ./frontend

The Angular Karma tests need a browser. Run them only in a container configured with the pinned Node version and Chromium; do not assume the production image build ran unit tests.

## Local Compose stack

Start or rebuild only the affected service when possible:

    docker compose up --build -d api
    docker compose up --build -d web
    docker compose up --build -d

Check the actual service names and health before smoke testing:

    docker compose ps
    docker compose logs --tail 80 api
    docker compose logs --tail 80 web
    docker compose logs --tail 80 database

Compose uses .env for local-only database and optional development-seed configuration. Never expose those values in logs or reports. Do not remove the persistent database volume as cleanup.
