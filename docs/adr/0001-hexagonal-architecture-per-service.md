# ADR-0001: Hexagonal architecture per service

## Status

Accepted

## Context

Every service (user, account, transaction, payment, fraud, notification, reporting, audit)
needs to keep its domain rules testable and independent of Spring, JPA, and Kafka, while
still being a normal Spring Boot application. The plan (`plan/ARCHITECTURE.md`) calls for
service-level DDD with hexagonal architecture rather than a transaction-script style where
controllers talk directly to repositories.

## Decision

Each service is split into:

- `domain`: aggregates, value objects, domain services — no Spring/JPA/Kafka imports.
- `application`: use cases (one class per command/query, e.g. `OpenAccountUseCase`,
  `FreezeAccountUseCase`) that orchestrate domain objects through ports.
- `port`: interfaces the application layer depends on (repository, event publisher, clock).
- `adapter/in`: REST controllers and Kafka `@KafkaListener`s that translate wire shapes into
  application calls.
- `adapter/out`: JPA repositories, Kafka producers, S3/SES clients that implement the ports.

## Consequences

- Domain and application logic can be unit tested with plain JUnit, no Spring context needed.
- Swapping an adapter (e.g. SES instead of SNS for notifications) doesn't touch application code.
- More files per feature than a transaction-script style — accepted as the cost of the
  above testability, and consistent across all 8 services so the navigation cost is fixed
  once, not per-service.
- Discovered a real hazard of this style during Phase 2/3 development: an application-layer
  class calling one of its own `@Transactional` methods via `this.method()` bypasses Spring's
  AOP proxy silently (see [ADR-0005](0005-self-invocation-transactional-bypass.md)). The fix reinforced
  the pattern rather than abandoning it — each transactional operation now gets its own
  single-purpose use-case bean, which also happens to be more hexagonal than the original
  one-class-many-methods design.
