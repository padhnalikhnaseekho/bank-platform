# Event-Driven Banking Platform

Production-grade planning repository for a Java 21, Spring Boot 3, Kafka, and AWS banking platform. The repository is designed for use with an agentic IDE and can be expanded into a runnable portfolio project in phases.

## Purpose

Build a realistic event-driven digital banking system that demonstrates:

- Java 21 and Spring Boot 3 microservices
- Domain-Driven Design and Hexagonal Architecture
- REST APIs, OpenAPI, validation, and global error handling
- Kafka eventing, Kafka Streams, Saga, CQRS, and Outbox patterns
- PostgreSQL, Redis, DynamoDB-oriented audit concepts, and S3 reporting
- AWS deployment patterns with RDS, S3, SQS, SNS, Secrets Manager, and CloudWatch
- Docker Compose, Kubernetes, Helm, Terraform, and GitHub Actions
- Observability with OpenTelemetry, Prometheus, Grafana, and structured logs
- Testing with JUnit 5, Mockito, Testcontainers, WireMock, and contract tests

## Repository Contents

| File | Purpose |
| --- | --- |
| `ROADMAP.md` | Phased implementation plan and milestone deliverables |
| `ARCHITECTURE.md` | System architecture, DDD boundaries, C4-style Mermaid diagrams |
| `SERVICES.md` | Detailed service responsibilities and internal module design |
| `KAFKA.md` | Topic model, event contracts, partitioning, retries, DLQ, outbox |
| `AWS.md` | AWS target architecture, service mapping, IAM, observability |
| `DATABASE.md` | PostgreSQL schema design, Redis usage, indexes, transaction rules |
| `API_SPEC.md` | REST API design for the main services |
| `INTERVIEW_QUESTIONS.md` | Java, Spring, Kafka, AWS, and system design questions |
| `IMPLEMENTATION_CHECKLIST.md` | Build checklist for tracking progress |
| `prompts/` | Agentic IDE prompts for implementation phases |

## Target Service Layout

```text
bank-platform/
  pom.xml
  common-library/
  user-service/
  account-service/
  transaction-service/
  payment-service/
  fraud-service/
  notification-service/
  reporting-service/
  audit-service/
  api-gateway/
  docker/
  kubernetes/
  terraform/
  observability/
  docs/
```

## Recommended Build Order

1. Create the multi-module Maven repository and shared libraries.
2. Implement User Service and Account Service with PostgreSQL and JWT.
3. Add Transaction Service with Kafka, idempotency, and Outbox.
4. Add Notification Service, Payment Service, Fraud Service, Audit Service, and Reporting Service.
5. Add AWS adapters and production deployment infrastructure.
6. Add observability, Kubernetes, CI/CD, and interview documentation.

## Local Development Stack

Initial Docker Compose dependencies:

- PostgreSQL
- Kafka and Kafka UI
- Redis
- LocalStack for AWS-compatible S3, SQS, SNS, and Secrets Manager
- Prometheus
- Grafana
- Jaeger or OpenTelemetry Collector

## Engineering Rules

- Use Java 21 records for immutable DTOs and event payloads.
- Keep domain logic independent from Spring annotations where practical.
- Use ports and adapters for persistence, messaging, cloud integrations, and external APIs.
- Publish integration events via Outbox, not directly from transaction methods.
- Use idempotency keys for money movement and externally submitted commands.
- Add tests with every service milestone.
- Keep service boundaries explicit. Avoid sharing entity classes across services.

## Milestone ZIP Names

- `bank-platform-v1-foundation.zip`
- `bank-platform-v2-transactions-kafka.zip`
- `bank-platform-v3-fraud-payments-reporting.zip`
- `bank-platform-v4-aws-infra.zip`
- `bank-platform-final.zip`

