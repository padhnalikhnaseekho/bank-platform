# ADR-0002: Transactional outbox pattern for reliable event publishing

## Status

Accepted

## Context

Money movement, account changes, and payment outcomes must be reflected in Kafka events for
downstream services (notification, audit, reporting, fraud) without ever losing an event or
publishing one for a database change that later rolled back. A naive "write to DB, then
publish to Kafka" sequence has a gap: the process can crash between the two writes, or the
Kafka publish can fail after the DB commit, silently dropping the event.

## Decision

Every service that emits domain events writes them to its own `outbox_events` table in the
same database transaction as the domain change (see `plan/DATABASE.md`). A separate
`OutboxPublisherJob` (in `common-library`) polls each service's outbox on a fixed schedule,
publishes pending rows to Kafka, and marks them published — all in one place, shared by every
service via `common-library`'s `EventPublisherAutoConfiguration`.

Consumers are made idempotent via `IdempotentEventProcessor`, which records the event id in a
per-service `idempotency_records`-style table before processing, so an outbox publisher retry
(at-least-once delivery) never double-applies an event.

## Consequences

- An event is only ever missing if the outbox row itself was never written, which happens
  inside the same transaction as the domain change — so "committed domain change without an
  event" and "event without a domain change" are both impossible.
- Adds latency between a domain change and its Kafka event (bounded by the publisher job's
  poll interval), which is acceptable for this platform's async event flows (notification,
  audit, reporting, fraud) — nothing here does synchronous read-your-own-write across services.
- Every service that emits events needs its own outbox table and Flyway migration; the cost is
  paid once per service since `common-library` supplies the publisher and idempotency
  machinery generically.
