# Implementation Checklist

## Foundation

- [x] Create Gradle parent project (plan called for Maven; built as a multi-module Gradle project instead)
- [x] Configure Java 21
- [x] Configure Spring Boot dependency management (Spring Boot 4.1.0, ahead of the Spring Boot 3 originally planned)
- [ ] Add Checkstyle or Spotless
- [x] Add Docker Compose
- [x] Add PostgreSQL container
- [x] Add Kafka container
- [x] Add Redis container (compose service present; no service consumes it yet — see plan/DATABASE.md's Redis Usage list)
- [x] Add LocalStack container
- [x] Add Prometheus, Grafana, and Jaeger
- [x] Add GitHub Actions build workflow

## Common Library

- [x] Error response model
- [x] Global exception utilities
- [x] Event envelope model
- [x] Correlation ID filter
- [x] Idempotency interfaces
- [x] Testcontainers base classes

## User Service

- [x] Register endpoint
- [x] Login endpoint
- [x] Refresh endpoint
- [x] JWT validation
- [x] Role model
- [x] Flyway migrations
- [x] Unit tests
- [x] Integration tests

## Account Service

- [x] Account aggregate
- [x] Money value object
- [x] Open account endpoint
- [x] Account query endpoints
- [x] Balance update application service
- [x] Ledger entry table
- [x] Optimistic locking tests
- [x] Concurrent transfer tests

## Transaction Service

- [x] Deposit command
- [x] Withdrawal command
- [x] Transfer command
- [x] Idempotency key handling
- [x] Transaction state machine
- [x] Outbox table
- [x] Outbox publisher
- [x] Kafka producer tests

## Kafka Consumers

- [x] Account Service transfer consumer
- [x] Notification Service consumer
- [x] Audit Service consumer
- [x] Retry configuration
- [x] DLQ configuration
- [x] Consumer idempotency table

## Payment Service

- [x] Payment instruction aggregate (one-time and recurring schedules)
- [x] Scheduled payment endpoint
- [x] Recurring payment endpoint
- [x] Cancel payment endpoint
- [x] Payment scheduler job (due-payment polling, publishes payment-due and triggers transfer-started)
- [x] Payment outcome listener (transfer-completed/transfer-failed -> payment-success/payment-failed)
- [x] Unit tests
- [x] Integration tests

## Fraud Service

- [x] Kafka Streams topology
- [x] Transfer count window rule
- [x] Transfer value window rule
- [x] Fraud alert producer
- [x] Topology tests
- [ ] New-payee-after-password-reset rule (listed as an example rule in plan/SERVICES.md; not yet implemented — no password-reset event exists in the catalog yet)
- [ ] Repeated-failed-login-then-high-value-transfer rule (same caveat)
- [x] Notification/Audit/Account consumers for fraud-alert (Notification emails the customer; Account freezes their active accounts pending review; Audit records it like every other event)

## Reporting Service

- [x] Materialized view consumer
- [x] Statement job model
- [x] CSV generation
- [x] PDF generation
- [x] S3 adapter
- [x] Report status API

## AWS and Infrastructure

See terraform/README.md for full detail, design rationale, and known simplifications.
Validated with `terraform init`/`validate`/`fmt` and a `plan` that got as far as an actual
AWS API call (proving the whole ~1,850-line module graph resolves and is acyclic) — never
`apply`'d against real AWS, since this environment has no AWS credentials to apply with.

- [x] Terraform VPC (public/private subnets across 2 AZs, IGW, NAT, route tables)
- [x] Terraform RDS (one shared Postgres instance, matches the app's existing
      one-schema-per-service design; RDS-managed master password via Secrets Manager)
- [x] Terraform S3 (statements/reports/audit-archive; encrypted, public access blocked,
      versioning + lifecycle on the audit archive)
- [x] Terraform SQS (two retry queues + shared dead-letter queue with redrive policies)
- [x] Terraform SNS (customer-notifications/fraud-alerts/operations-alerts topics)
- [x] Terraform Secrets Manager (JWT signing RSA key pair, provider-credential
      placeholders — see terraform/README.md's caveat: JwtKeyConfig.java still needs a
      follow-up change to actually read this instead of self-generating a key every restart)
- [x] Terraform ElastiCache Redis and MSK Serverless (Kafka) — in the AWS.md target mapping
      but not originally itemized in this checklist; added since ECS services need both
- [x] ECS Fargate deployment (Cloud Map service discovery for internal calls, ALB fronting
      only api-gateway, one task definition per service)
- [x] IAM least privilege policies (per-service execution + task roles; only
      reporting-service's task role has a real grant today, matching what application code
      actually calls — see services.tf's comments)

## Observability and Resilience

- [ ] JSON logs
- [x] Correlation ID propagation (CorrelationIdFilter for HTTP, EventProcessingContext for Kafka consumers)
- [ ] OpenTelemetry traces
- [ ] Prometheus metrics
- [ ] Grafana dashboards
- [ ] Resilience4j retry
- [ ] Circuit breaker
- [ ] Rate limiter
- [ ] Load tests

