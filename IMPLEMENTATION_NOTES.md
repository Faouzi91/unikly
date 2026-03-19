# Implementation Notes — Production Hardening

These notes supplement the SRS with implementation-level concerns that
Claude Code must follow when generating code.

## 1. Outbox Pattern Hardening

- The OutboxPublisher @Scheduled poller must NOT call .get() synchronously
  on kafkaTemplate.send(). Use the async CompletableFuture callback instead
  to avoid blocking the poller thread and holding DB transactions open.
- OutboxRepository.findByStatusOrderByCreatedAtAsc must use Pageable with
  a LIMIT (default: 100 per poll cycle). Never fetch all pending events
  at once — if Kafka is down, the outbox table grows unbounded and the
  poller will OOM.
- For future scale: evaluate Debezium CDC as a replacement for the polling
  outbox. This is a Phase 2 optimization, not required for initial launch.

## 2. Idempotent Consumer in Search Service

- The search-service indexes into Elasticsearch and records processed
  events in a separate store. These are two non-transactional operations.
- To mitigate: always save to Elasticsearch FIRST (using the eventId as
  the document ID — ES upserts are naturally idempotent), THEN record
  the event in processed_events. If the second write fails, the next
  retry will upsert the same ES document (safe) and then succeed on
  the processed_events write.
- For services using PostgreSQL for both domain data and processed_events:
  wrap both writes in a single @Transactional block. This is already safe.

## 3. JVM Memory in Docker Containers

- Every Java service Dockerfile ENTRYPOINT must include JVM memory flags:
  ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-XX:+UseG1GC",
  "-XX:MaxGCPauseMillis=200", "-jar", "app.jar"]
- Alternatively, set JAVA_TOOL_OPTIONS=-Xms256m -Xmx512m as an
  environment variable in docker-compose.yml.
- Container memory limits (deploy.resources.limits.memory) in
  docker-compose must be ~25% higher than -Xmx to account for
  non-heap JVM memory (metaspace, thread stacks, NIO buffers).

## 4. Kafka Producer Configuration

- All Kafka producers must set acks=all for guaranteed delivery.
- Set retries=3 with retry.backoff.ms=1000.
- Set enable.idempotence=true on producers to prevent duplicate
  messages on retry.

Commit this file now.
