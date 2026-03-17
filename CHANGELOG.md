# Changelog

All notable changes to Unikly will be documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Shared event records (11 events) for inter-service communication
- Money, MatchEntry, PageResponse DTOs
- OutboxEvent JPA entity with scheduled publisher for guaranteed delivery
- UserContext security utility
- Docker Compose with all infrastructure — 7 PostgreSQL instances, Kafka 4.2.0
  (KRaft, no ZooKeeper), Keycloak 26.5.5, Elasticsearch 9.0, Redis 7.4,
  Prometheus, Grafana
- Kafka topics auto-created on startup (job.events, payment.events,
  matching.events, messaging.events, user.events, notification.dlq, payment.dlq)

## [0.1.0] - 2026-03-17

### Added
- Initialized monorepo structure with backend (Gradle multi-module), frontend,
  matching-ai, and infra directories
- Created root Gradle build with Spring Boot 4.0.3, Java 25 toolchain, and
  shared dependencies
- Created common module for shared DTOs and event classes
- Added .gitignore, .env.example, and README.md
