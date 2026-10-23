# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 4.1 / Java 25 backend for an "Enterprise AI Assistant". Currently (Wave 1) a REST API for
customers and orders on PostgreSQL; a local LLM with read-only tools is planned for later waves
(see the roadmap in `README.md`). The late-orders query is meant to become the LLM's first tool.

## Rules
- Never change a migration already existing (migration starting with V* in db/migration). You can produce V<n+1> migration
- Never use LocalDate.now() or Instant.now() without Clock
- Before declaring closed a task, run ./mvnw verify and read failures/errors
- Whenever you write a new functionality, write his test
- For every new test: temporarily break the code under test, confirm the test fails, then restore it
- Request DTOs never contain ids, status or dates the server decides (use the Clock)
- Ask before destructive commands (docker compose down -v, git push --force, git reset --hard, deleting files)
- For changes touching more than one file, propose a plan and wait for approval before editing
- at the end of the task, explain what you have done

## Commands

Use the Maven Wrapper (`./mvnw`; on Windows PowerShell `.\mvnw`). Docker must be running for tests and for `spring-boot:run`.

```bash
./mvnw verify                                        # full build + all tests (Testcontainers starts PostgreSQL)
./mvnw test -Dtest=OrderServiceTest                  # single test class
./mvnw test -Dtest=OrderServiceTest#findLateOrdersReturnsEmptyListWhenNothingIsLate   # single test method
./mvnw spring-boot:run                               # runs with the "dev" profile; starts PostgreSQL via compose.yaml
docker compose down -v                               # DESTRUCTIVE: wipes the local dev database volume
```

Swagger UI: `http://localhost:8080/swagger-ui.html`.

## Architecture

Package-by-feature under `io.github.arcangelotanzi.assistant`:

- `customer/`, `order/` — each holds its entity, repository, service, controller, request/response DTOs and domain exceptions.
- `common/` — `GlobalExceptionHandler`, `ResourceNotFoundException` (base class of all "not found" exceptions), `TimeConfig` (the `Clock` bean).

Layering: controller → service → repository. Controllers only see DTOs (records, converted with static `from(...)`
factories); entities never leave the service layer. Services are `@Transactional(readOnly = true)` at class level,
with `@Transactional` on writing methods. Errors are thrown as domain exceptions and translated to RFC 9457
`ProblemDetail` in `GlobalExceptionHandler` (which extends `ResponseEntityExceptionHandler`, so it handles all MVC errors).

### Persistence

- Flyway owns the schema (`src/main/resources/db/migration`); Hibernate runs with `ddl-auto: validate`.
- Demo data lives in a repeatable migration under `db/seed`, loaded only by the `dev` profile (`application-dev.yaml`,
  activated for `spring-boot:run` in the `pom.xml`). Tests and production apply `db/migration` only.
- Database `CHECK` constraints mirror domain rules (e.g. `delivered_at` set iff status is `DELIVERED`).

### Time

"Today" always comes from the injected `Clock` (business zone from `app.time-zone`, `Europe/Rome`).

### Tests

- Unit tests: plain JUnit + Mockito; services are built manually with a `Clock.fixed(...)`.
- Web tests: `@WebMvcTest` + `MockMvcTester` + `@MockitoBean`.
- Repository tests: `@DataJpaTest` + `@Import(TestcontainersConfiguration.class)` (real PostgreSQL 18.6).
- End-to-end: `ApiIntegrationTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`, fixed `Clock` via `@Primary`, cleanup in `@AfterEach`).

Spring Boot 4 moved test annotations into per-module packages (e.g. `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`,
`org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`); Jackson 3 uses the `tools.jackson` package.
