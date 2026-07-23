# ADR-0011: Explicit bulkhead sizing for HTTP, JDBC, Kafka, and scheduled-task pools

## Status

Accepted

## Context

Every service ran on Spring Boot's implicit defaults for all of its thread and connection
pools: an unbounded-looking 200-thread Tomcat pool, HikariCP's default pool size, one Kafka
consumer thread per `@KafkaListener` container, and — critically — a single-threaded
`@Scheduled` task executor shared by every scheduled method in the JVM.

That last default surfaced a real, concrete bug while auditing this: `common-library`'s
`OutboxPublisherJob.publishOne()` called `kafkaTemplate.send(...).get()` with **no timeout**.
In any service with only one scheduled task, a hung Kafka send just delays that one job's next
run. But payment-service has two: the outbox publisher and `PaymentSchedulerJob` (the
due-payment poller), and Spring Boot's default scheduling pool size is 1 — both compete for the
same thread. A Kafka outage would have blocked that single thread on `.get()` forever, silently
stopping due-payment polling too, with no exception, no log, nothing — just payments that quietly
stop firing.

## Decision

- Fixed the actual bug: `OutboxPublisherJob.publishOne()` now calls `.get(timeout, unit)` with a
  10-second default (configurable via a constructor overload), and a failed/timed-out send falls
  back to `e.getClass().getSimpleName()` when `e.getMessage()` is null (as it is for a plain
  `TimeoutException`) so the outbox's `failure_reason` column is never just `"null"`.
- `payment-service` gets an explicit `spring.task.scheduling.pool.size: 2`, since it's the one
  service actually running two scheduled tasks that would otherwise contend for one thread.
- Every service with a datasource gets an explicit `spring.datasource.hikari.maximum-pool-size`
  (15) and `connection-timeout` (3000ms) — declaring the pool size that was previously an
  implicit Hikari default, and failing fast instead of queuing indefinitely when the pool is
  exhausted.
- Every Kafka-consuming service gets an explicit `spring.kafka.listener.concurrency` (3), so
  consumer thread count is a known, bounded quantity instead of implicitly growing with
  partition count as topics scale.
- `fraud-service` (Kafka Streams, not `@KafkaListener`) gets the equivalent knob:
  `num.stream.threads: 2`.
- Every service gets an explicit `server.tomcat.threads.max` — 100 for internal services, 150
  for `api-gateway` specifically, since it's the single fan-in point for all inbound traffic and
  deserves more headroom than a service that only receives gateway-routed requests.

## Consequences

- The one real bug this uncovered (the unbounded outbox `.get()`) is fixed regardless of the
  rest of this ADR's sizing choices — that fix alone would have been worth doing on its own.
- Every pool's size is now a deliberate, documented number instead of "whatever the framework
  defaults to," which is what actually makes this a bulkhead: each resource class (HTTP
  threads, DB connections, Kafka consumer threads, scheduled-task threads) has its own
  independent, bounded capacity, so exhausting one can't silently exhaust another.
- The specific numbers (15 DB connections, 3 Kafka consumer threads, 100 HTTP threads) are
  starting points sized for this platform's current traffic, not load-tested figures — revisit
  them if a real bottleneck shows up in the Grafana dashboards (JVM/connection-pool metrics are
  already scraped, see `docker/grafana/provisioning/dashboards/bank-platform-overview.json`).
- This does **not** give each resource class a fully separate pool per *caller* (e.g. HTTP
  requests and the outbox job still share the same HikariCP pool within one service) — that
  finer-grained isolation isn't idiomatic for a Spring Boot monolith-per-service architecture
  like this one, and wasn't needed to fix the actual bug found. If a specific service's outbox
  job or scheduled task ever needs isolation from its own HTTP request load specifically, that
  would be the next increment, not a wholesale redesign.
