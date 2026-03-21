#!/bin/bash
set -e

echo "=== Building backend ==="
cd backend && ./gradlew clean bootJar && cd ..

echo "=== Building frontend ==="
cd frontend/unikly-app && npm ci && npx ng build --configuration production && cd ../..

echo "=== Building Java builder base image ==="
docker build -f infra/docker/Dockerfile.java-builder -t localhost/unikly/java-builder:latest .

echo "=== Building Docker images ==="
cd infra && docker compose build

echo "=== Build complete ==="
