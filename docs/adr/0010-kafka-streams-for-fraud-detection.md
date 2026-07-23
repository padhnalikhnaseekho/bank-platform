# ADR-0010: Kafka Streams for fraud detection, not a plain `@KafkaListener`

## Status

Accepted

## Context

Fraud rules need to reason about a customer's recent transfer *history* — a sliding count and
sum of transfer value over a time window (`HighTransferCountRule`, `HighTransferValueRule`) —
not just the single incoming event. A plain `@KafkaListener` consumer has no built-in notion of
"what happened for this key in the last N minutes"; that state would have to be reconstructed
from the database on every event, or kept in a hand-rolled in-memory map with no fault
tolerance.

## Decision

`fraud-service` builds a Kafka Streams topology (`FraudDetectionTopologyBuilder`) that
consumes `transfer-completed`, re-keys by `sourceCustomerId`, aggregates into a
`SlidingWindows` `KTable` backed by a `WindowStore` (`transfer-window-stats-store`), and emits
`fraud-alert` when a rule's `evaluate(...)` trips. The topology-building logic is kept as a
plain static method with no Spring dependency specifically so it can be exercised directly with
`TopologyTestDriver` in tests, with `FraudStreamsConfig` doing the Spring wiring separately.

## Consequences

- Windowed aggregation state (the sliding count/sum per customer) is Kafka Streams'
  responsibility — it's fault-tolerant (backed by a changelog topic) and rebuilds automatically
  on restart, instead of being reimplemented and manually persisted.
- Fraud rules (`FraudRule` implementations) stay simple, stateless functions over
  `TransferWindowStats` — new rules plug in as another `FraudRule` bean without touching the
  windowing/aggregation plumbing.
- Cost: fraud-service needs its own dedicated changelog/repartition topics and a
  `TopologyTestDriver`-based test setup distinct from every other service's plain
  `@KafkaListener` + `EmbeddedKafka`/Testcontainers tests — accepted because the windowing
  requirement is also unique to this service.
- Two rules from `plan/SERVICES.md`'s example list (new-payee-after-password-reset,
  repeated-failed-login-then-high-value-transfer) aren't implemented: both need events
  (password-reset, failed-login) that don't exist in the event catalog yet, not a topology
  limitation.
