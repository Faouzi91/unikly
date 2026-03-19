#!/bin/bash
set -e

echo "=== Building backend ==="
cd backend && ./gradlew clean bootJar && cd ..

echo "=== Building frontend ==="
cd frontend/unikly-app && npm ci && npx ng build --configuration production && cd ../..

echo "=== Building Docker images ==="
cd infra && docker compose build

echo "=== Build complete ==="
