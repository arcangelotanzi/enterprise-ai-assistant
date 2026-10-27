---
description: Creates the next versioned Flyway migration for a schema change, following the project's migration rules. Use when the user asks to add or change tables, columns, constraints or indexes.
argument-hint: <schema change to make>
disable-model-invocation: true
---

## Requested change

$ARGUMENTS

## Existing versioned migrations

!`ls src/main/resources/db/migration`

## Instructions

1. **Version number**: take the highest `V<n>` in the list above and use `n + 1`. Never reuse or skip a number.
2. **File name**: `src/main/resources/db/migration/V<n+1>__<snake_case_description>.sql`, with **two** underscores
   after the version (Flyway silently ignores files with one).
3. **Content**:
   - PostgreSQL syntax. Name every constraint and index explicitly: `pk_`, `fk_`, `uk_`, `ck_`, `ix_` + table + purpose.
   - Index every new foreign key column (PostgreSQL does not do it automatically).
   - New `NOT NULL` columns on existing tables need a `DEFAULT` or a backfill, otherwise the migration fails on existing rows.
   - Add a short SQL comment only where the reason is not obvious.
4. **Never** edit an existing `V*` migration, not even to fix a typo: correct it in the new migration.
   The repeatable seed `db/seed/R__dev_seed_data.sql` is the only migration that may be edited; update it if the
   change needs new demo data or breaks the existing inserts.
5. **Keep Java in sync**: if a column or table maps to an entity, update the entity too (`ddl-auto: validate`
   fails at startup on any mismatch). Ask before changing entities, DTOs or the API.
6. **Verify**: run `./mvnw spotless:apply` then `./mvnw verify`, and report `Tests run / Failures / Errors`.
   Do not run `./mvnw spring-boot:run`: it would apply the migration to the persistent dev database. 
6b. **Check against the request**: re-read the requested change and compare it with the SQL
   line by line (types, lengths, nullability, names). Quote both in the report.
7. **Report**: the new file name, the SQL, the Java files changed, and anything you did not verify. Do not commit.
