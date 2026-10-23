# Enterprise AI Assistant

A conversational assistant for business data, built with **Java 25** and **Spring Boot 4**.
It answers questions such as *"Which orders are late, and which customers should I contact?"*
by letting a **local LLM** call a small set of **authorized, read-only tools**. The model never
queries the database directly.

> **Status: Wave 1 complete — REST backend.** Customers and orders are exposed through a tested
> REST API on PostgreSQL, including the late-orders query the assistant will use as its first tool.
> **The LLM is not integrated yet** (Wave 3). The roadmap table is the source of truth for what works.

## Design principles

- **The LLM never touches the database.** It can only request typed, validated, read-only tools; the application decides and executes.
- **Zero mandatory costs.** Everything runs locally with open-source tools (Ollama, PostgreSQL, Minikube).
- **Provider-agnostic.** The LLM sits behind an interface, so the model can be swapped.
- **Measured, not claimed.** Latency, errors, tool usage and tokens are observable.

## Roadmap

| Wave | Scope | Status |
|------|-------|--------|
| 0 | Dev environment, repository, build | ✅ Done |
| 1 | REST backend: customers & orders, PostgreSQL, migrations, tests | ✅ Done |
| 2 | AI-assisted development workflow (CLAUDE.md, skills, hooks) | Planned |
| 3 | Local LLM integration (Ollama + Spring AI) | Planned |
| 4 | Tool calling & bounded agent loop | Planned |
| 5 | MCP server in Java (read-only tools) | Planned |
| 6 | Enterprise architecture: adapters, caching, ADRs | Planned |
| 7 | Observability: metrics, tracing, dashboards, evaluation | Planned |
| 8 | Docker, Docker Compose, Kubernetes (Minikube), CI | Planned |
| 9 | Portfolio polish & reproducible demo | Planned |

## Tech stack

| Area | Choice |
|------|--------|
| Language | Java 25 (Eclipse Temurin, LTS) |
| Framework | Spring Boot 4.1, Spring Framework 7 |
| Persistence | PostgreSQL 18, Spring Data JPA (Hibernate 7), Flyway 12 |
| API | Spring MVC, Bean Validation, RFC 9457 ProblemDetail errors |
| API docs | springdoc-openapi 3.1 (OpenAPI 3 + Swagger UI) |
| Testing | JUnit 6, AssertJ, Mockito, Spring `MockMvcTester`, Testcontainers 2 |
| Build | Maven (via Maven Wrapper) |
| Local services | Docker Compose, started automatically by Spring Boot |

## Getting started

**Prerequisites:** JDK 25 and Docker (Docker Desktop on Windows/macOS). Maven is not required: the wrapper downloads it.

```bash
git clone https://github.com/arcangelotanzi/enterprise-ai-assistant.git
cd enterprise-ai-assistant
```

**Run the tests.** Docker must be running: integration tests start a throwaway PostgreSQL with Testcontainers.

```bash
./mvnw verify          # Windows PowerShell: .\mvnw verify
```

**Run the application.** Spring Boot starts PostgreSQL from `compose.yaml`, Flyway creates the schema,
and the `dev` profile loads demo data (dates are relative to the day the data is loaded).

```bash
./mvnw spring-boot:run
```

- API: <http://localhost:8080/api/customers>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

**Reset the local database** (for example, to reload demo data with fresh dates):

```bash
docker compose down -v
```

## API

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| `GET` | `/api/customers` | List customers | `200` |
| `GET` | `/api/customers/{id}` | Get a customer | `200` |
| `POST` | `/api/customers` | Create a customer | `201` + `Location` |
| `GET` | `/api/orders/{id}` | Get an order | `200` |
| `POST` | `/api/orders` | Create an order (dated today by the server) | `201` + `Location` |
| `GET` | `/api/orders/late` | Open orders past their promised delivery date, oldest first | `200` |

**Late orders** — everything needed to decide whom to contact, without further calls
(one element of a real response from the demo data):

```json
{
  "orderId": 8,
  "customerId": 4,
  "customerName": "Giovannino S.p.a.",
  "customerEmail": "contatti@giovanninospa.example",
  "customerPhone": "+39 9876543210",
  "status": "SHIPPED",
  "promisedDeliveryDate": "2026-10-13",
  "daysLate": 10,
  "totalAmount": 2100.00
}
```

An order is **late** when it is still open (not `DELIVERED` or `CANCELLED`) and its promised
delivery date is before today. An order due today is not late yet.

**Errors** use the RFC 9457 `application/problem+json` format:

| Situation | Status |
|-----------|--------|
| Invalid input (with per-field `errors`) | `400` |
| Customer or order not found | `404` |
| Email already used | `409` |
| Promised delivery date in the past | `422` |

```json
{"title": "Not Found", "status": 404, "detail": "No customer found with id 999", "instance": "/api/customers/999"}
```

## Testing

45 automated tests across four layers:

| Layer | Examples | What it proves |
|-------|----------|----------------|
| Unit (plain JUnit + Mockito) | `OrderTest`, `OrderServiceTest`, `CustomerServiceTest` | Domain rules and service logic, including time-zone edge cases on a fixed clock |
| Web (`@WebMvcTest`) | `CustomerControllerTest`, `OrderControllerTest` | Status codes, headers, JSON shape, validation and error format |
| Repository (`@DataJpaTest` + PostgreSQL) | `CustomerRepositoryTest`, `OrderRepositoryTest` | Queries, mapping and constraints on a real database, and that late orders load customers in **one** SQL query |
| End-to-end (`@SpringBootTest` + PostgreSQL) | `ApiIntegrationTest` | The full path HTTP → service → database → JSON, and the generated OpenAPI document |

## Key design decisions

- **Schema owned by Flyway, validated by Hibernate** — migrations are versioned code; `ddl-auto: validate` fails fast if entities drift from the schema.
- **Database constraints as the last line of defence** — status values, non-negative totals and "delivered iff `delivered_at` is set" are enforced in SQL, not only in Java.
- **Injectable `Clock` in the business time zone (`Europe/Rome`)** — "today" is a dependency, so tests run on a fixed date and the late-orders logic stays correct around midnight.
- **Partial index for late orders** — only open orders are indexed, matching the query's `WHERE` clause.
- **N+1 prevented and proven** — `@EntityGraph` loads customers with the orders; a test counts SQL statements with Hibernate statistics (1 with the graph, 4 without).
- **DTOs at the boundary** — entities never leave the service layer; requests carry only what the client may decide (the server sets order date and status).
- **Demo data only in `dev`** — a repeatable Flyway migration loaded by a profile; tests and production apply the schema only.
- **Deterministic messages** — a fixed `en` locale keeps validation messages identical on every machine and in CI.

## License

[MIT](LICENSE)
