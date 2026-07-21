# Roadmap

## Phase 0: Repository Foundation

Goal: create the skeleton that all later services follow.

Deliverables:

- Multi-module Maven project with Java 21 and Spring Boot 3
- Parent dependency management
- Shared `common-library` for errors, event metadata, tracing helpers, and test utilities
- Docker Compose for PostgreSQL, Kafka, Redis, LocalStack, Prometheus, Grafana, and Jaeger
- GitHub Actions build pipeline
- Baseline README, architecture docs, and ADR template

Acceptance criteria:

- `mvn clean verify` runs successfully
- Docker Compose starts all dependencies
- Each service has health endpoints
- Each service exposes OpenAPI docs

## Phase 1: Identity and Accounts

Goal: support customer registration, login, and account opening.

Services:

- User Service
- Account Service
- API Gateway

Features:

- Register customer
- Login with JWT
- Refresh token
- Role-based authorization
- Open savings/current account
- View account details and balance
- Account lifecycle status: pending, active, frozen, closed

Engineering topics:

- Spring Security
- BCrypt
- JWT
- Validation
- PostgreSQL migrations
- Optimistic locking
- Global exception handling
- Structured logs and correlation IDs

Acceptance criteria:

- User can register and log in
- Authenticated user can open an account
- OpenAPI documents all endpoints
- Unit and integration tests cover happy path and validation failures

## Phase 2: Transactions and Kafka

Goal: implement reliable money movement and event-driven communication.

Services:

- Transaction Service
- Notification Service
- Audit Service, initial version

Features:

- Deposit
- Withdraw
- Internal transfer
- Idempotency key support
- Transaction status state machine
- Kafka producers and consumers
- Outbox publisher
- Notification events
- Audit event capture

Engineering topics:

- Saga basics
- Outbox pattern
- Kafka topic design
- Consumer groups
- Retry and DLQ
- Transactional boundaries
- Pessimistic and optimistic locking

Acceptance criteria:

- Duplicate transfer requests do not double debit
- Account balances remain consistent under concurrent transfers
- Outbox rows are eventually published to Kafka
- Failed consumer messages are retried then sent to DLQ

## Phase 3: Payments, Fraud, Reporting

Goal: add richer business workflows and streaming analytics.

Services:

- Payment Service
- Fraud Service
- Reporting Service

Features:

- Scheduled payments
- Recurring payments
- Kafka Streams fraud detection
- Sliding window transfer rules
- Fraud alert topic
- Statement generation
- CSV and PDF report generation
- S3 upload via LocalStack locally

Engineering topics:

- Kafka Streams windows and state stores
- CQRS materialized views
- Spring Scheduling
- S3 adapter
- Rule strategy pattern
- Report generation

Acceptance criteria:

- Fraud Service detects high-value and burst-transfer scenarios
- Reporting Service creates monthly statements
- Reports are uploaded to S3-compatible storage
- Payment scheduler emits payment events

## Phase 4: AWS and Infrastructure

Goal: provide a credible cloud deployment path.

Deliverables:

- Terraform modules
- ECS or EKS deployment option
- RDS PostgreSQL
- ElastiCache Redis
- MSK or self-managed Kafka option
- S3 buckets
- SQS retry and DLQ queues
- SNS notification topics
- Secrets Manager integration
- CloudWatch dashboards and alarms

Acceptance criteria:

- Terraform plan describes all required infrastructure
- Services read secrets from environment or Secrets Manager abstraction
- IAM policy examples follow least privilege
- Deployment guide includes local, staging, and production paths

## Phase 5: Production Hardening

Goal: make the project interview-ready and portfolio-ready.

Deliverables:

- Kubernetes manifests and Helm charts
- OpenTelemetry tracing
- Prometheus metrics and Grafana dashboards
- Resilience4j circuit breaker, retry, bulkhead, and rate limiter
- Load tests with k6 or Gatling
- Contract tests
- Architecture Decision Records
- Interview guide mapped to code

Acceptance criteria:

- End-to-end tests exercise registration, account opening, transfer, fraud alert, notification, and reporting
- Dashboards show request rate, error rate, latency, Kafka lag, and JVM metrics
- CI runs unit and integration tests
- Documentation explains tradeoffs and failure modes

