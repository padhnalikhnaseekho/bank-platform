# Implementation Checklist

## Foundation

- [x] Create Gradle parent project (plan called for Maven; built as a multi-module Gradle project instead)
- [x] Configure Java 21
- [x] Configure Spring Boot dependency management (Spring Boot 4.1.0, ahead of the Spring Boot 3 originally planned)
- [x] Add Checkstyle or Spotless (Spotless + google-java-format in AOSP mode, 4-space indent to
      match the existing style; applied to all 11 modules via a `subprojects {}` block in the
      root build.gradle.kts rather than repeating config per module; `spotlessCheck` runs as
      part of `check`/`build`, so CI now fails on unformatted code)
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

- [x] JSON logs (Spring Boot 4.1 native structured logging, ECS format, no extra dependency —
      `logging.structured.format.console: ecs` in all 10 services)
- [x] Correlation ID propagation (CorrelationIdFilter for HTTP, EventProcessingContext for Kafka consumers)
- [x] OpenTelemetry traces (spring-boot-starter-opentelemetry, OTLP export to Jaeger; verified
      end-to-end including auto-instrumented @Scheduled spans — outbox publisher job showed up
      as a real trace)
- [x] Prometheus metrics (micrometer-registry-prometheus + /actuator/prometheus permitAll'd
      across all 7 secured services; verified live scrape shows target "up" for all 9 app services)
- [x] Grafana dashboards (one overview dashboard, 7 panels: service up, HTTP request rate,
      HTTP 5xx rate, HTTP p95 latency, JVM heap, JVM GC pause, Kafka consumer lag; provisioned
      via docker/grafana/provisioning, verified live)
- [ ] Resilience4j retry — skipped; the codebase has no custom synchronous inter-service HTTP
      calls to wrap (only Spring Security's transparent JWKS fetch), so there's no natural
      application point for it today
- [ ] Circuit breaker — same reasoning as retry
- [x] Bulkhead (`docs/adr/0011-bulkhead-isolation.md` — not originally itemized in this
      checklist but is one of `plan/ROADMAP.md`'s Phase 5 deliverables; explicit HikariCP pool
      sizing, Kafka listener/streams thread counts, Tomcat thread caps, and a fix for a real bug
      this surfaced: `OutboxPublisherJob`'s Kafka send had no timeout, so on a Kafka outage it
      could hang the single scheduling thread payment-service shares between the outbox
      publisher and its due-payment poller, forever)
- [x] Rate limiter (api-gateway only, the one place it's clearly meaningful regardless of
      internal call patterns) — Redis-backed sliding-window-log limiter (one bucket per client
      IP, 100 req/60s default, 429 on exhaustion), see
      `docs/adr/0012-redis-backed-rate-limiter.md`; started as an in-memory Resilience4j
      limiter (`docs/adr/0009`, superseded) before Redis was wired into the platform for real;
      verified live via k6 and by inspecting the Redis sorted set directly after triggering 429s
- [x] Load tests (k6, `load-tests/money-movement.js` — register/login/open account/deposit/read
      status through the gateway); verified live end-to-end, see `load-tests/README.md` for the
      rate-limiter caveat when running at higher VU counts
- [x] Kubernetes manifests / Helm chart (`helm/bank-platform/` — not originally itemized in this
      checklist but is one of the Phase 5 deliverables in `plan/ROADMAP.md`; a portable
      alternative to the Terraform/ECS deployment, not a replacement for it — see
      [ADR-0006](../docs/adr/0006-ecs-fargate-over-eks.md); `helm lint`/`helm template` verified
      clean, not `helm install`'d against a live cluster)
- [x] Contract tests (`*ContractTest.java` in account/transaction/fraud/notification/payment/
      reporting-service, backed by shared JSON fixtures in
      `common-library/src/testFixtures/resources/contracts/`) — this repo has each event's DTO
      hand-copied into every consumer independently (discovered while building this: 6 separate
      `TransferOutcomeEvent` definitions across 5 services), so these tests are a real gap-filler,
      not a formality; producer-side tests assert the actual serialized payload matches the
      fixture, consumer-side tests assert each service's own DTO can still read it
- [x] Architecture Decision Records (`docs/adr/`, 10 ADRs covering hexagonal architecture, the
      outbox pattern, schema-per-service, Kafka topic-per-event-type, the self-invocation
      `@Transactional` bug, ECS vs EKS, MSK Serverless, the gateway's WebMVC vs WebFlux choice,
      and the in-memory rate limiter)
- [x] Interview guide mapped to code (`plan/INTERVIEW_QUESTIONS.md` — all 60 questions now carry
      a pointer to the file/class that answers them, or an explicit note where no anchor exists
      in this repo, e.g. `ConcurrentHashMap` and sealed classes are never used here)

