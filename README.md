# Enterprise AI Assistant

A conversational assistant for business data, built with **Java 25** and **Spring Boot 4**.
It answers questions such as *"Which orders are late, and which customers should I contact?"*
by letting a **local LLM** call a small set of **authorized, read-only tools**. The model never
queries the database directly.

> **Status: Wave 0 — project skeleton.** Most features below are *planned*, not implemented yet.
> The roadmap table is the source of truth for what actually works.

## Design principles

- **The LLM never touches the database.** It can only request typed, validated, read-only tools; the application decides and executes.
- **Zero mandatory costs.** Everything runs locally with open-source tools (Ollama, PostgreSQL, Minikube).
- **Provider-agnostic.** The LLM sits behind an interface, so the model can be swapped.
- **Measured, not claimed.** Latency, errors, tool usage and tokens are observable.

## Roadmap

| Wave | Scope | Status |
|------|-------|--------|
| 0 | Dev environment, repository, build | 🚧 In progress |
| 1 | REST backend: customers & orders, PostgreSQL, migrations, tests | Planned |
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
| Framework | Spring Boot 4.1 |
| Build | Maven (via Maven Wrapper) |

## Getting started

**Prerequisites:** JDK 25. Maven is not required: the wrapper downloads it.

```bash
git clone https://github.com/<your-username>/enterprise-ai-assistant.git
cd enterprise-ai-assistant
./mvnw verify          # Windows PowerShell: .\mvnw verify
```

## License

[MIT](LICENSE)
