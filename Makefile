.PHONY: dev dev-logs dev-monitor prod build down clean test status

## Start all services in development mode
dev:
	cd infra && docker compose up -d

## Tail logs for a specific service (e.g. make dev-logs SERVICE=gateway)
dev-logs:
	cd infra && docker compose logs -f $(SERVICE)

## Start services + monitoring stack (Prometheus, Grafana, Tempo)
dev-monitor:
	cd infra && docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

## Start all services in production mode (resource limits, nginx, restart policies)
prod:
	cd infra && docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

## Build all Docker images (backend JAR → frontend dist → Docker images)
build:
	./build.sh

## Stop all running containers
down:
	cd infra && docker compose down

## Stop all containers, remove volumes and local images
clean:
	cd infra && docker compose down -v --rmi local

## Run backend integration and unit tests
test:
	cd backend && ./gradlew test

## Show current container status
status:
	cd infra && docker compose ps
