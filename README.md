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

**Status**: Phase 0 (foundation) and Phase 1 (identity and accounts) are done.

- `user-service`: registration, login, refresh tokens, RS256-signed JWTs, JWKS endpoint at
  `/.well-known/jwks.json`.
- `account-service`: open/get/list/freeze accounts, optimistic locking, ownership
  enforcement (a customer can only see their own accounts; freezing is admin-only).
- `api-gateway`: routes `/api/v1/users/**` and `/api/v1/auth/**` to `user-service`,
  `/api/v1/accounts/**` to `account-service`; independently validates JWTs at the gateway
  too (defense in depth, not just trusting the route).
- Everything else (`transaction-service`, `payment-service`, `fraud-service`,
  `notification-service`, `reporting-service`, `audit-service`, `ai-service`) is still the
  Phase 0 skeleton — compiling and bootable, no business logic yet.

Event publishing is stubbed (`common-library`'s `NoOpEventPublisher`) — the roadmap assigns
the real Kafka/Outbox wiring to Phase 2.

## Local end-to-end flow

```bash
docker compose -f docker/docker-compose.yml up -d postgres
./gradlew :user-service:bootRun &
./gradlew :account-service:bootRun &
./gradlew :api-gateway:bootRun &

curl -X POST localhost:8080/api/v1/users/register -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","fullName":"Alice","password":"StrongPassword123!"}'
curl -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"StrongPassword123!"}'
# use the returned accessToken as a Bearer token against /api/v1/accounts via the gateway
```

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
