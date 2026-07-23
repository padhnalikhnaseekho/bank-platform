# Architecture Decision Records

Short records of the significant architectural decisions made while building this platform,
in the lightweight Status/Context/Decision/Consequences format. Numbered in the order they were
written, not necessarily the order the decisions were made.

- [0001 - Hexagonal architecture per service](0001-hexagonal-architecture-per-service.md)
- [0002 - Transactional outbox pattern for reliable event publishing](0002-transactional-outbox-pattern.md)
- [0003 - One schema per service in a shared PostgreSQL instance](0003-schema-per-service-shared-postgres.md)
- [0004 - One Kafka topic per event type, with idempotent consumers](0004-kafka-topic-per-event-type-idempotent-consumers.md)
- [0005 - Extract `@Transactional` logic into separate beans, never call it via `this`](0005-self-invocation-transactional-bypass.md)
- [0006 - ECS Fargate over EKS for compute](0006-ecs-fargate-over-eks.md)
- [0007 - MSK Serverless over self-hosted Kafka on AWS](0007-msk-serverless-over-self-hosted-kafka.md)
- [0008 - Spring Cloud Gateway Server WebMVC over WebFlux](0008-gateway-server-webmvc-over-webflux.md)
- [0009 - In-memory Resilience4j rate limiter at the gateway](0009-in-memory-rate-limiter.md) (superseded by 0012)
- [0010 - Kafka Streams for fraud detection](0010-kafka-streams-for-fraud-detection.md)
- [0011 - Explicit bulkhead sizing for HTTP, JDBC, Kafka, and scheduled-task pools](0011-bulkhead-isolation.md)
- [0012 - Redis-backed sliding-window-log rate limiter](0012-redis-backed-rate-limiter.md)
