---
name: postgres-flyway-architect
description: >-
  Design and review Unikly PostgreSQL 18 schemas, queries, JPA persistence, and immutable Flyway migrations. Use for database changes, constraints, indexes, or query behavior.
---

# PostgreSQL and Flyway architect

Follow the schema and query conventions already present in Unikly. Review the application query and domain invariant along with every schema change.

## Safe Flyway changes

- Migrations live in backend/src/main/resources/db/migration/ and use V<N>__description.sql with the next available positive integer.
- Treat every applied migration as immutable. Never edit, rename, or delete an existing migration; add a forward migration.
- Check existing schema, seed data, entity mappings, and queries before designing a migration.
- Name constraints and indexes clearly. Add foreign keys, NOT NULL, CHECK, and uniqueness constraints where they represent real domain invariants.
- Choose ON DELETE behavior to match the owning domain lifecycle. Make sure required foreign keys are populated by migration and seed data.
- For a populated table, plan safe backfill and constraint ordering. Do not silently drop or rewrite production data.
- For large or continuously written tables, assess lock time and use a staged expand, backfill, and contract rollout when application compatibility requires it.
- Do not use IF NOT EXISTS to hide schema drift or make an incorrect migration appear successful.

## Types and query design

- Use NUMERIC/DECIMAL for money, never floating point. Match the precision and scale to current domain limits and Java mappings.
- Use TIMESTAMPTZ for instants. Use bounded VARCHAR for values with a real domain limit and TEXT for free-form descriptions.
- Match indexes to observed predicates, joins, sort order, and pagination. For composite B-tree indexes, put equality predicates before range or ordering columns when that fits the query.
- Account for index storage and write cost; do not add speculative indexes. Index foreign keys when actual joins or deletes need them.
- Avoid N+1 loading and unbounded result sets. Review JPA fetch strategy and the SQL generated for important read paths.
- Keep SQL parameterized. Preserve database constraints as the final guard for concurrency-sensitive invariants such as uniqueness and stock changes.

## Verification

- Unit tests that use H2 do not prove PostgreSQL-specific SQL or migration behavior.
- For database behavior changes, use the project Java 25 Docker workflow, then exercise the affected path against the local PostgreSQL Compose stack when the project workflow requires it.
- Use EXPLAIN (ANALYZE, BUFFERS) only against safe representative local data when evaluating a real query; never claim a performance improvement from an index definition alone.
- Do not print .env values, credentials, user data, or production connection strings.
