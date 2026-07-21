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
- [ ] Optimistic locking tests
- [ ] Concurrent transfer tests

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
- [ ] Notification/Audit/Account consumers for fraud-alert (per plan/KAFKA.md's topic table; fraud-alert is produced but nothing consumes it yet)

## Reporting Service

- [x] Materialized view consumer
- [x] Statement job model
- [x] CSV generation
- [x] PDF generation
- [x] S3 adapter
- [x] Report status API

## AWS and Infrastructure

- [ ] Terraform VPC
- [ ] Terraform RDS
- [ ] Terraform S3
- [ ] Terraform SQS
- [ ] Terraform SNS
- [ ] Terraform Secrets Manager
- [ ] ECS or EKS deployment
- [ ] IAM least privilege policies

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

