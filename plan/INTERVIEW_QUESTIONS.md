# Interview Questions

Each question has a `→` pointer to where this repo actually demonstrates (or deliberately
doesn't use) the concept, added after the code existed to make this a study guide grounded in
real decisions rather than a generic checklist. See also `docs/adr/` — several of these
pointers are ADRs specifically because the reasoning behind the decision is the more
interesting answer than the code itself.

## Java 21

1. Explain the difference between `record`, class, and Lombok DTO.
   → `account-service/.../domain/Account.java` (plain class) vs `domain/AccountId.java`
   (record id); no Lombok anywhere in this repo.
2. When would you use `Optional` and when would you avoid it?
   → `account-service/.../application/GetAccountUseCase.java` (`findById().orElseThrow()`).
3. Explain Java memory model visibility issues in concurrent balance updates.
   → `account-service/src/test/.../ConcurrentTransferIntegrationTest.java` (10 virtual threads
   fan into one account, asserts no lost update).
4. How does `ConcurrentHashMap` differ from `HashMap` with synchronization?
   → No anchor — `ConcurrentHashMap` isn't used anywhere in this codebase; open discussion.
5. Explain virtual threads and where they help in Spring Boot services.
   → `account-service/src/test/.../ConcurrentTransferIntegrationTest.java`
   (`Executors.newVirtualThreadPerTaskExecutor()`).
6. What are sealed classes and how could they model transaction commands?
   → No anchor — no `sealed` classes in this repo; command-like types (`TransactionType`,
   `PaymentStatus`) are plain enums. Worth discussing as a possible refactor.
7. Explain equals and hashCode pitfalls in JPA entities.
   → `account-service/.../adapter/out/persistence/AccountEntity.java` (`@Id`/`@Version`, no
   `equals()`/`hashCode()` override — the pitfall itself, live in the codebase).
8. How do streams differ from loops in performance and readability?
   → `account-service/.../adapter/in/web/dto/AccountListResponse.java`
   (`page.getContent().stream().map(...).toList()`).
9. Explain checked versus unchecked exceptions in service boundaries.
   → `common-library/.../error/DomainException.java` (unchecked, extends `RuntimeException`)
   vs `common-library/.../error/JsonAuthenticationEntryPoint.java` (declares checked
   `IOException`).
10. What causes deadlocks and how can lock ordering reduce them?
    → `account-service/.../application/ApplyTransferUseCase.java` +
    `AccountOptimisticLockingIntegrationTest.java` — optimistic locking with retry was chosen
    over ordered pessimistic locks specifically to sidestep this.

## Spring Boot

1. What happens when a request enters a Spring MVC controller?
   → `account-service/.../adapter/in/web/AccountController.java` +
   `common-library/.../web/CorrelationIdFilter.java` (filter chain into the controller).
2. Explain `@Transactional` propagation and isolation.
   → [`docs/adr/0005-self-invocation-transactional-bypass.md`](../docs/adr/0005-self-invocation-transactional-bypass.md)
   + `account-service/.../application/ApplyTransferUseCase.java` — a real bug this project hit
   and fixed, not a textbook example.
3. Why should domain objects avoid direct dependency on Spring annotations?
   → [`docs/adr/0001-hexagonal-architecture-per-service.md`](../docs/adr/0001-hexagonal-architecture-per-service.md)
   + `account-service/.../domain/Account.java` (plain POJO, no Spring imports).
4. How does Spring Security validate JWT tokens?
   → `api-gateway/.../config/SecurityConfig.java` +
   `common-library/.../security/JwtAuthenticationAutoConfiguration.java` (shared
   JWT-to-authority converter).
5. What is the difference between authentication and authorization?
   → `user-service/.../application/LoginUseCase.java` (authentication) vs
   `account-service/.../AccountController.java`'s `@PreAuthorize("hasRole('ADMIN')")`
   (authorization).
6. How does Spring Data JPA implement repositories?
   → `account-service/.../adapter/out/persistence/AccountJpaRepository.java` (derived-query
   `JpaRepository` interface).
7. Explain lazy loading and the N+1 query problem.
   → `user-service/.../adapter/out/persistence/UserEntity.java`
   (`@ManyToMany(fetch = EAGER)` roles) vs `CredentialEntity.java`
   (`@OneToOne(fetch = LAZY)`).
8. How do profiles help local, test, and AWS deployments?
   → No `@Profile`/profile-specific files exist — this repo uses `${DB_URL:...}`-style env-var
   defaults in `application.yml` instead (see any service's `application.yml`, and
   `terraform/modules/ecs-service/main.tf` for how AWS injects different values), which is
   itself a worthwhile contrast to draw out in an answer.
9. What does Spring Actuator expose?
   → any service's `application.yml`
   (`management.endpoints.web.exposure.include: health,info,prometheus`).
10. How do you implement global exception handling?
    → `common-library/.../error/GlobalExceptionHandler.java`.

## Kafka

1. Explain topic, partition, offset, and consumer group.
   → `transaction-service/.../adapter/in/messaging/TransactionOutcomeListener.java`
   (`@KafkaListener(topics = ...)`) + each service's `application.yml` `group-id`.
2. Why does partition key choice matter?
   → `common-library/.../event/EventEnvelope.java` (`partitionKey` field) +
   `fraud-service/.../FraudDetectionTopologyBuilder.java` (`selectKey` by customerId).
3. How do you make a Kafka consumer idempotent?
   → `common-library/.../event/IdempotentEventProcessor.java` +
   `common-library/.../event/ProcessedEventStore.java`.
4. What is at-least-once delivery and how does it affect money movement?
   → `common-library/.../event/KafkaErrorHandlingAutoConfiguration.java` (retry + DLT) +
   `IdempotentEventProcessor.java` (makes redelivery safe).
5. How does the Outbox pattern prevent lost events?
   → `common-library/.../event/OutboxPublisherJob.java` +
   [`docs/adr/0002-transactional-outbox-pattern.md`](../docs/adr/0002-transactional-outbox-pattern.md).
6. When would you use Kafka Streams instead of plain consumers?
   → `fraud-service/.../adapter/streams/FraudDetectionTopologyBuilder.java` +
   [`docs/adr/0010-kafka-streams-for-fraud-detection.md`](../docs/adr/0010-kafka-streams-for-fraud-detection.md).
7. Explain retry topics and dead-letter topics.
   → `common-library/.../event/KafkaErrorHandlingAutoConfiguration.java` (exponential backoff
   into a shared `dead-letter` topic).
8. How do you evolve event schemas safely?
   → `common-library/.../event/EventEnvelope.java` (`eventVersion` field) — and, less happily,
   the contract tests under `*ContractTest.java` in account/transaction/fraud/notification/
   payment/reporting-service, which exist precisely because this repo has 6 independently
   hand-written copies of some event DTOs with no shared schema enforcement otherwise.
9. What is consumer lag and how do you monitor it?
   → `docker/grafana/provisioning/dashboards/bank-platform-overview.json`
   ("Kafka Consumer Lag" panel).
10. Explain exactly-once semantics and its limits.
    → No EOS code in this repo — `common-library/.../event/IdempotentEventProcessor.java` is
    the at-least-once-plus-idempotency alternative actually used, which is itself a good talking
    point about why EOS is often more than a system needs.

## Database and Transactions

1. Explain optimistic versus pessimistic locking.
   → `account-service/.../adapter/out/persistence/AccountEntity.java` (`@Version`) +
   `AccountOptimisticLockingIntegrationTest.java`.
2. What isolation level would you use for account balance updates?
   → `account-service/.../domain/Account.java` + `AccountEntity.java` (`@Version`) — optimistic
   locking was chosen here over a stricter isolation level.
3. Why should every balance update create a ledger entry?
   → `account-service/.../domain/Account.java`'s `debit()`/`credit()` +
   `domain/LedgerEntry.java` + the `ledger_entries` table in
   `account-service/src/main/resources/db/migration/V1__init.sql`.
4. How would you design indexes for statement queries?
   → `reporting-service/src/main/resources/db/migration/V1__init.sql`
   (`idx_account_activity_view_account_id_occurred_at`).
5. What is a transaction boundary in a Saga?
   → `payment-service/.../application/TriggerPaymentUseCase.java` (its own `@Transactional`
   bean, publishes `payment-due`) — see also System Design Q1 for the fuller saga.
6. How do you handle duplicate external requests?
   → `transaction-service/.../application/IdempotencyGuard.java` (Idempotency-Key hash check +
   unique DB index).
7. What causes phantom reads?
   → No concrete anchor here — open/conceptual discussion.
8. When would you denormalize reporting data?
   → `reporting-service/.../domain/AccountActivityEntry.java` +
   `adapter/in/messaging/AccountActivityEventListener.java` (the denormalized
   `account_activity_view`, built async from Kafka events).
9. How do database migrations work in CI/CD?
   → any service's `src/main/resources/db/migration/V1__init.sql` (Flyway,
   `ddl-auto: validate`) + `.github/workflows/build.yml`.
10. Why should services not share database tables?
    → [`docs/adr/0003-schema-per-service-shared-postgres.md`](../docs/adr/0003-schema-per-service-shared-postgres.md).

## AWS

1. Compare ECS Fargate and EKS for this platform.
   → [`docs/adr/0006-ecs-fargate-over-eks.md`](../docs/adr/0006-ecs-fargate-over-eks.md).
2. Why put services in private subnets?
   → `terraform/modules/vpc/main.tf` (`aws_subnet.private`) +
   `terraform/environments/dev/vpc.tf`.
3. How should services access Secrets Manager?
   → `terraform/modules/secrets/main.tf` + `terraform/modules/ecs-service/main.tf`
   (`execution_secrets` IAM policy).
4. What CloudWatch alarms matter for a banking API?
   → No CloudWatch alarms are provisioned in this repo's Terraform — the closest existing
   monitoring is the Grafana/Prometheus stack in `docker/`, worth naming explicitly as a
   different tool than the question asks about, and a real gap if this went to production AWS.
5. When would you use SQS instead of Kafka?
   → `terraform/environments/dev/messaging.tf` (`sqs` retry queues alongside the `msk` module).
6. How do S3 lifecycle policies help audit archive cost?
   → `terraform/modules/s3/main.tf` (`aws_s3_bucket_lifecycle_configuration`) +
   `terraform/environments/dev/storage.tf` (audit-archive bucket, 90-day glacier transition).
7. What IAM permissions does Reporting Service need?
   → `terraform/environments/dev/services.tf` (`reporting_service_task` IAM doc:
   `s3:GetObject`/`PutObject` — the only service with a real task-role grant today).
8. How does RDS Multi-AZ improve availability?
   → `terraform/modules/rds/main.tf` (`multi_az` variable on `aws_db_instance`).
9. What is the role of security groups?
   → `terraform/environments/dev/network-rules.tf` (DB/Kafka client security-group
   allow-lists, added after every ECS service module exists — see the module comments on why).
10. How would you deploy with zero downtime?
    → `terraform/modules/ecs-service/main.tf` (`aws_ecs_service`) +
    `terraform/modules/alb/main.tf` (`health_check` block).

## System Design

1. Design a reliable money transfer flow.
   → `transaction-service/.../application/CreateTransferUseCase.java` +
   `account-service/.../adapter/in/messaging/TransferListener.java`/`ApplyTransferUseCase.java`
   — a choreographed saga over Kafka, not a 2PC.
2. How do you prevent double debit on retry?
   → `transaction-service/.../application/IdempotencyGuard.java` +
   `AccountOptimisticLockingIntegrationTest.java` +
   `common-library/.../event/IdempotentEventProcessor.java` — three different layers, each
   guarding a different retry path (HTTP client retry, concurrent write, Kafka redelivery).
3. How do you detect fraud in near real time?
   → `fraud-service/.../adapter/streams/FraudDetectionTopologyBuilder.java` +
   `application/rule/HighTransferCountRule.java`/`HighTransferValueRule.java`.
4. What happens if Kafka is temporarily unavailable?
   → `common-library/.../event/OutboxPublisherJob.java` — events sit in the DB outbox and keep
   retrying until publish succeeds; nothing is lost.
5. How does the system recover from a failed notification provider?
   → `notification-service/.../domain/DeliveryAttempt.java` +
   `adapter/out/channel/*Adapter.java` + `terraform/environments/dev/messaging.tf`
   (`notification-retry-queue`).
6. What data belongs in audit logs?
   → `audit-service/.../adapter/in/messaging/AuditEventListener.java` (subscribes to every
   topic) + `domain/AuditEvent.java`.
7. How would you support 10x transaction volume?
   → No concrete anchor — no autoscaling config exists in this repo; open/conceptual
   discussion (ECS Fargate task count, RDS read replicas, MSK Serverless's own scaling are all
   fair starting points, but none are configured here).
8. Which operations must be strongly consistent?
   → `account-service/.../application/ApplyTransferUseCase.java` + `AccountEntity.java`
   (`@Version`) — debit/credit happen in one DB transaction, strongly consistent by
   construction.
9. Which operations can be eventually consistent?
   → `reporting-service/.../adapter/in/messaging/AccountActivityEventListener.java` — the
   `account_activity_view` is built asynchronously from Kafka, consistent only eventually.
10. How would you explain this architecture to a non-technical stakeholder?
    → No code anchor — open discussion. The system-context diagram in
    `plan/ARCHITECTURE.md` is the closest thing to a non-technical starting point.
