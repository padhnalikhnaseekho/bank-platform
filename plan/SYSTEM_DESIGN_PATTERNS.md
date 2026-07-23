# System Design Patterns Analysis

**Source:** _System Design Interview_ by Alex Xu

**Last Updated:** 2026-07-23

---

## Executive summary

An earlier version of this document (kept at
[`SYSTEM_DESIGN_PATTERNS_ORIGINAL.md`](SYSTEM_DESIGN_PATTERNS_ORIGINAL.md) for reference, not
deleted) significantly understated what's actually implemented — it described the platform as
if it were still in an early Phase 0/1 state (no outbox, no idempotency, no Kafka Streams, a
hallucinated synchronous Account↔Transaction HTTP call used to justify circuit breakers). All of
that has been verified against the current code and corrected below. The one gap that earlier
version correctly identified — bulkhead/thread-pool isolation — is now fixed. The rest of this
document is a straight status report of Alex Xu-style patterns against what's actually in this
repo, plus a separate, honest section on what else from that book would be genuinely worth
applying here.

See [`SYSTEM_DESIGN_PATTERNS_PRACTICE_PLAN.md`](SYSTEM_DESIGN_PATTERNS_PRACTICE_PLAN.md) for a
set of concrete features to build as practice exercises for each pattern.

**Current status:** 13/13 patterns considered are implemented or deliberately, explicitly
scoped out with a documented reason. Nothing is silently missing.

---

## Patterns implemented

### 1. Microservices architecture
10 independently deployable services (user, account, transaction, payment, fraud,
notification, reporting, audit, ai, api-gateway), each with a single bounded context, defined
in `settings.gradle.kts`.

### 2. API gateway
`api-gateway/` — centralized routing (`GatewayRoutesConfig`), JWT validation as
defense-in-depth alongside each downstream service's own validation, and a rate limiter (see
#5 below). Built on Spring Cloud Gateway Server WebMVC, not the reactive gateway — see
[ADR-0008](../docs/adr/0008-gateway-server-webmvc-over-webflux.md) for why.

### 3. Database per service (schema per service, in practice)
Each service owns a Postgres **schema** (`account_service`, `transaction_service`, ...) inside
one shared RDS instance rather than a fully separate database instance per service — a
deliberate cost/ops tradeoff for this scale, not the textbook "one database per service."
See [ADR-0003](../docs/adr/0003-schema-per-service-shared-postgres.md).

### 4. Asynchronous communication via Kafka, with the Outbox pattern
**This is fully implemented, not stubbed.** `common-library`'s `OutboxPublisherJob` +
`OutboxEventPublisherAdapter` write events to a per-service `outbox_events` table in the same
transaction as the domain change, then a scheduled job publishes them to Kafka. See
[ADR-0002](../docs/adr/0002-transactional-outbox-pattern.md).

There is a `NoOpEventPublisher` in `common-library`, but it is **only** a
`@ConditionalOnMissingBean` fallback for services with no outbox table at all (e.g.
fraud-service, which publishes via its Kafka Streams topology directly, and ai-service, which
publishes nothing) — every service that actually needs transactional event publishing gets the
real outbox-backed `EventPublisher`.

### 5. Rate limiting at the gateway
A Redis-backed sliding-window-log limiter, one bucket per client IP, 100 req/60s default, 429 on
exhaustion — verified live (Redis sorted-set contents inspected directly after triggering 429s).
See [ADR-0012](../docs/adr/0012-redis-backed-rate-limiter.md), which replaced the original
in-memory Resilience4j fixed-window limiter ([ADR-0009](../docs/adr/0009-in-memory-rate-limiter.md))
specifically to fix its two documented limitations: fixed-window boundary bursting, and state
that didn't survive multiple gateway replicas.

### 6. Idempotency and deduplication
Two independent layers, each guarding a different kind of retry:
- **Client retry of the same command**: `transaction-service`'s `IdempotencyGuard` hashes the
  request body against the `Idempotency-Key` header and returns the cached response on a
  duplicate.
- **Kafka redelivery**: `common-library`'s `IdempotentEventProcessor` records an event's id
  before processing and skips it on redelivery (necessary because outbox delivery is
  at-least-once, not exactly-once — see [ADR-0004](../docs/adr/0004-kafka-topic-per-event-type-idempotent-consumers.md)).
- **Concurrent writes to the same account**: optimistic locking (`@Version` on `AccountEntity`)
  plus a retry loop, verified under real concurrency load in
  `AccountOptimisticLockingIntegrationTest`/`ConcurrentTransferIntegrationTest`
  (virtual-thread fan-out).

### 7. Kafka Streams for real-time aggregation
`fraud-service`'s `FraudDetectionTopologyBuilder` — a real, tested sliding-window topology
(`SlidingWindows`, a `WindowStore`-backed `KTable`), not a plan. See
[ADR-0010](../docs/adr/0010-kafka-streams-for-fraud-detection.md).

### 8. Saga pattern (choreography)
The money-transfer flow is a working choreographed saga over Kafka:
`CreateTransferUseCase` (transaction-service) publishes `transfer-started` →
`TransferListener`/`ApplyTransferUseCase` (account-service) debits/credits and publishes
`transfer-completed`/`transfer-failed` → fraud, notification, reporting, and audit all react
independently. No orchestrator, no saga runtime library — each step is a normal Kafka consumer,
which is the right amount of machinery for a saga this shape. See **Saga compensation** below
for the one real, deliberate gap in this pattern.

### 9. CQRS-flavored read model
`reporting-service`'s `AccountActivityEventListener` denormalizes events from Kafka into an
`account_activity_view` table, queried independently of the write-side services — a real (if
partial) CQRS read model, not a plan.

### 10. Observability
Prometheus, Grafana, Jaeger/OpenTelemetry, JSON structured logs with correlation IDs — all
verified live end-to-end this session, not just wired up. See the Observability section of
`plan/IMPLEMENTATION_CHECKLIST.md`.

### 11. JWT authentication, defense in depth
User-service issues RS256 JWTs and exposes `/.well-known/jwks.json`; every resource server
(including the gateway) validates independently rather than trusting an upstream
already-validated header.

### 12. Health checks
Spring Boot Actuator `/actuator/health` on every service, used by both Docker Compose
healthchecks and (per the Helm chart) Kubernetes readiness/liveness probes.

### 13. Bulkhead / thread-pool isolation
**Was the one real gap; now implemented.** See `docs/adr/0011-bulkhead-isolation.md` for what
was actually isolated and why (Kafka consumer concurrency capped independently of the HTTP
request thread pool and the JDBC connection pool, so a slow consumer can't starve request
handling).

---

## Saga compensation: documented as deliberate, not implemented as auto-reversal

A fraud alert does **not** automatically reverse the transfer that triggered it. Instead,
`FreezeAccountsForFraudAlertUseCase` freezes every ACTIVE account belonging to the flagged
customer, for manual review.

This is not an oversight — it's mechanically the only option available today, and arguably the
safer choice regardless:

- `TransferWindowStats` (what a `FraudRule` actually evaluates) only tracks an aggregate
  **count** and **total amount** per customer per sliding window — it does not retain which
  specific transfer(s) pushed the customer over a rule's threshold. Auto-reversing "the
  transfer that caused this" isn't possible without first changing the windowed aggregation to
  retain individual transfer IDs, which is a real design change, not a quick addition.
- Even if that data existed, auto-reversal is a double-edged pattern in a real banking system:
  it's an attack surface of its own (an attacker who can trigger false positives can trigger
  reversals) and it can misfire on a legitimate customer's unusual-but-real activity. Freezing
  the account and routing to human review is the more conservative, defensible default.

If this were to change, the model would be: extend `TransferWindowStats` to collect triggering
transfer IDs, add a `ReverseTransferUseCase` in account-service, and have
`FreezeAccountsForFraudAlertUseCase` call it per triggering transfer — with its own
authorization/audit trail given the sensitivity of an automated reversal.

---

## My own analysis: other Alex Xu patterns worth considering here

Patterns from the book that would be reasonable to bring up in an interview conversation about
this system, evaluated honestly against what this platform actually needs at its current scale
— not a padded wish list.

### Done (see `SYSTEM_DESIGN_PATTERNS_PRACTICE_PLAN.md`)

**Rate limiter algorithm choice.** The original gateway limiter was closer to a fixed-window
counter than the sliding-window-log or token-bucket algorithms Alex Xu's rate limiter chapter
covers — it reset the full quota at each `limitRefreshPeriod` boundary, meaning a client could
burst up to 2x the nominal limit across a boundary. Rebuilt as a Redis-backed sliding-window-log
limiter — see [ADR-0012](../docs/adr/0012-redis-backed-rate-limiter.md).

### Worth doing

**Unique ID generation and index locality.** Every entity uses `UUID.randomUUID()` (UUIDv4).
That's fine for uniqueness but UUIDv4 has no time-ordering, which matters specifically for
`ledger_entries` — a hot-insert, append-mostly table where a monotonically-increasing key (like
a Snowflake-style ID, or Postgres's own `bigserial`, or UUIDv7 once broadly available in the
JDK/Postgres driver stack in use) would keep new rows physically clustered near the end of the
index rather than scattered across it. This is a genuine, specific interview-relevant
observation about this codebase's actual ledger table, not a generic "use Snowflake IDs" note.

**Read replica for reporting queries.** `reporting-service` already has a denormalized read
model (`account_activity_view`) but it lives in the same RDS instance as every write-side
service. Alex Xu's read/write splitting pattern applies cleanly here: reporting's queries are
read-only, latency-tolerant (already eventually consistent by design, since they're populated
from Kafka), and could point at an RDS read replica instead of the primary — isolating
reporting's query load from the write path entirely. `terraform/modules/rds` doesn't provision
a replica today; this would be a real, scoped addition if reporting load ever became a concern.

### Worth naming, not worth building yet

**Sharding / partitioning the database.** Not remotely needed at this platform's scale (a
single shared RDS instance handles this comfortably); worth being able to articulate when it
would become necessary (write throughput or dataset size outgrowing a single primary) rather
than pre-building it.

**Consistent hashing.** Already present in spirit, just not by that name: Kafka's own
partition-key-based routing (`partitionKey` on `EventEnvelope`, keyed by `accountId`/
`customerId`/`transferId` depending on the topic) is a real-world instance of hash-based
routing for ordering guarantees. No separate consistent-hashing layer is needed since Kafka
already owns that problem.

**CDN / edge caching.** Not applicable — this platform has no static asset or public
content-serving surface; every endpoint is an authenticated API call.

### Explicitly not needed

**A distributed consensus layer (Raft/Paxos, leader election) of our own.** Kafka (via its own
internal Raft-based controller quorum, KRaft) and RDS already own the consensus problems this
platform has. Building another one would be solving a problem that doesn't exist here.
