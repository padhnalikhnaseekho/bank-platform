# ADR-0004: One Kafka topic per event type, with idempotent consumers

## Status

Accepted

## Context

Money movement, transfers, payments, and fraud alerts fan out to multiple consumers
(notification, reporting, audit, and sometimes other domain services) as listed in
`plan/KAFKA.md`. Kafka delivery in this design is at-least-once (see
[ADR-0002](0002-transactional-outbox-pattern.md)'s outbox publisher retry behavior), so
consumers can see the same event more than once.

## Decision

Each domain event gets its own topic named after the event (`money-deposited`,
`transfer-completed`, `fraud-alert`, ...) rather than one wide topic with a `type` field, and
every consumer wraps its handler with `IdempotentEventProcessor` (`common-library`), which
records the envelope's `eventId` before processing and skips it if already seen.

## Consequences

- A new consumer for an existing event subscribes to exactly the topic it needs, with no
  filtering logic for unrelated event types.
- Topic count grows with event-type count (16 topics today, per `plan/KAFKA.md`) — accepted
  because each topic can be reasoned about, retried, and monitored (consumer lag) independently.
- At-least-once delivery plus idempotent consumers means a redelivered event after a consumer
  crash-and-restart is a correctness non-issue instead of a double-processing bug — this is
  what let the fraud-alert consumers added to Notification/Account/Audit be written without
  any special-casing for duplicate delivery.
- Ordering is only guaranteed within a partition, so partition keys (`accountId`, `transferId`,
  `customerId`) are chosen per topic specifically to keep events that must stay ordered on the
  same partition.
