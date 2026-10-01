---
name: postgres-flyway-architect
description: >-
  Database architecture, PostgreSQL 18 standards, and Flyway schema migration best practices.
  Use when designing database schemas, writing Flyway migrations, defining foreign keys, or debugging SQL queries.
---

# PostgreSQL & Flyway Architect Skill

This skill documents database standards, schema design principles, and Flyway migration practices for Unikly's PostgreSQL 18 database.

## Flyway Migration Standards

1. **File Naming & Location**:
   - Directory: `backend/src/main/resources/db/migration/`
   - Pattern: `V<VERSION>__<description>.sql` (e.g., `V13__fix_aluminum_pencil_set_image.sql`).
   - Use double underscores `__` between the version number and the description.
   - Version numbers must be sequential positive integers.

2. **Migration Immutability**:
   - **Never alter or delete an existing migration file that has already run in any environment.**
   - Flyway calculates a SHA-256 checksum for each applied migration. Editing an existing migration causes `DbValidate` failure upon application restart.
   - Always create a new forward migration (`V<N+1>__...sql`) to apply fixes or schema updates.

3. **Data Type Conventions**:
   - **Monetary amounts**: Always use `DECIMAL(10, 2)` or `NUMERIC(10, 2)` (never `FLOAT`, `REAL`, or `DOUBLE PRECISION`).
   - **Timestamps**: Always use `TIMESTAMPTZ` or `TIMESTAMP WITH TIME ZONE` defaulting to `CURRENT_TIMESTAMP`.
   - **Strings & IDs**: Use `VARCHAR` with explicit character bounds (e.g. `VARCHAR(255)`, `VARCHAR(64)`), or `TEXT` for free-form descriptions.
   - **Foreign Keys**: Always define explicit `REFERENCES` and appropriate `ON DELETE` clauses. Ensure required relations have `NOT NULL` constraints (such as `seller_id` in `customer_order_items`).

---

## Safe Database Access via Container

To inspect or query the running PostgreSQL database without installing local client tools on the host:

```bash
docker exec -it unikly-database-1 psql -U unikly_dev -d unikly
```

Or execute inline queries:
```bash
docker exec unikly-database-1 psql -U unikly_dev -d unikly -c "SELECT version();"
docker exec unikly-database-1 psql -U unikly_dev -d unikly -c "SELECT version, description, success FROM flyway_schema_history;"
```
