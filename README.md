# Event-Driven Banking Platform

Java 21, Spring Boot 4.1, Spring Framework 7, Kafka, and AWS-oriented banking platform.
Design docs, roadmap, and architecture live in [`plan/`](plan/README.md) — this file
covers running the code.

## Version note (deviation from `plan/`)

The planning docs in `plan/` describe a Spring Boot 3 stack. This implementation targets
**Spring Boot 4.1.x / Spring Framework 7** instead, because the platform includes an
`ai-service` module built on **Spring AI 2.0**, which requires Boot 4.1+. Everything else
in `plan/` (service boundaries, Kafka topics, API shapes, roadmap phases) still applies.

## Modules

| Module | Port | Responsibility |
| --- | --- | --- |
| `api-gateway` | 8080 | Routing, JWT validation, rate limiting |
| `user-service` | 8081 | Registration, login, JWT, refresh tokens |
| `account-service` | 8082 | Account lifecycle, balance, ledger |
| `transaction-service` | 8083 | Deposits, withdrawals, transfers, idempotency |
| `payment-service` | 8084 | Scheduled and recurring payments |
| `fraud-service` | 8085 | Kafka Streams fraud rule evaluation |
| `notification-service` | 8086 | Email, SMS, push delivery |
| `reporting-service` | 8087 | Statements and reports |
| `audit-service` | 8088 | Immutable audit trail, compliance search |
| `ai-service` | 8089 | Spring AI 2.0 home for LLM-backed capabilities |
| `common-library` | — | Shared, dependency-only building blocks |

Phase 0 status: each service is a compiling, bootable Spring Boot skeleton with
`/actuator/health` and OpenAPI (`/v3/api-docs`, `/swagger-ui.html`) wired up. No business
logic yet — that starts in Phase 1 (`plan/ROADMAP.md`).

## Prerequisites

- Java 21 (the Gradle wrapper below handles Gradle itself)
- Docker + Docker Compose

## Build

```bash
./gradlew build
```

## Run local infrastructure

```bash
docker compose -f docker/docker-compose.yml up -d
```

Starts PostgreSQL, Kafka (+ Kafka UI on :8090), Redis, LocalStack (S3/SQS/SNS/Secrets
Manager), Prometheus, Grafana, and Jaeger.

```bash
docker compose -f docker/docker-compose.yml down
```

## Run a service

```bash
./gradlew :user-service:bootRun
curl localhost:8081/actuator/health
```

Swap `user-service` and `8081` for any module in the table above.
