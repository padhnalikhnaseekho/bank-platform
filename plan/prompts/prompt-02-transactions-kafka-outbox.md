# Prompt 02: Transactions, Kafka, and Outbox

Implement Transaction Service and Kafka-based event publishing.

Requirements:

- Deposit endpoint
- Withdrawal endpoint
- Transfer endpoint
- Idempotency key support
- Transaction lifecycle state machine
- Outbox table and publisher
- Kafka event envelope
- Kafka producer configuration
- Consumer idempotency support in common-library

Events:

- `transaction-created`
- `transfer-started`
- `transfer-completed`
- `transfer-failed`
- `money-deposited`
- `money-withdrawn`

Acceptance criteria:

- Duplicate requests with the same idempotency key return the original result.
- Outbox events are stored in the same transaction as state changes.
- Publisher marks events as published only after Kafka send succeeds.
- Tests use Testcontainers for PostgreSQL and Kafka.

