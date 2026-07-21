# Implementation Checklist

## Foundation

- [ ] Create Maven parent project
- [ ] Configure Java 21
- [ ] Configure Spring Boot 3 dependency management
- [ ] Add Checkstyle or Spotless
- [ ] Add Docker Compose
- [ ] Add PostgreSQL container
- [ ] Add Kafka container
- [ ] Add Redis container
- [ ] Add LocalStack container
- [ ] Add Prometheus, Grafana, and Jaeger
- [ ] Add GitHub Actions build workflow

## Common Library

- [ ] Error response model
- [ ] Global exception utilities
- [ ] Event envelope model
- [ ] Correlation ID filter
- [ ] Idempotency interfaces
- [ ] Testcontainers base classes

## User Service

- [ ] Register endpoint
- [ ] Login endpoint
- [ ] Refresh endpoint
- [ ] JWT validation
- [ ] Role model
- [ ] Flyway migrations
- [ ] Unit tests
- [ ] Integration tests

## Account Service

- [ ] Account aggregate
- [ ] Money value object
- [ ] Open account endpoint
- [ ] Account query endpoints
- [ ] Balance update application service
- [ ] Ledger entry table
- [ ] Optimistic locking tests
- [ ] Concurrent transfer tests

## Transaction Service

- [ ] Deposit command
- [ ] Withdrawal command
- [ ] Transfer command
- [ ] Idempotency key handling
- [ ] Transaction state machine
- [ ] Outbox table
- [ ] Outbox publisher
- [ ] Kafka producer tests

## Kafka Consumers

- [ ] Account Service transfer consumer
- [ ] Notification Service consumer
- [ ] Audit Service consumer
- [ ] Retry configuration
- [ ] DLQ configuration
- [ ] Consumer idempotency table

## Fraud Service

- [ ] Kafka Streams topology
- [ ] Transfer count window rule
- [ ] Transfer value window rule
- [ ] Fraud alert producer
- [ ] Topology tests

## Reporting Service

- [ ] Materialized view consumer
- [ ] Statement job model
- [ ] CSV generation
- [ ] PDF generation
- [ ] S3 adapter
- [ ] Report status API

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
- [ ] Correlation ID propagation
- [ ] OpenTelemetry traces
- [ ] Prometheus metrics
- [ ] Grafana dashboards
- [ ] Resilience4j retry
- [ ] Circuit breaker
- [ ] Rate limiter
- [ ] Load tests

