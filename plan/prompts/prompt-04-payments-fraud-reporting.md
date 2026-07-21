# Prompt 04: Payments, Fraud, and Reporting

Implement Payment Service, Fraud Service, and Reporting Service.

Payment Service:

- Scheduled payments
- Recurring payments
- Payment attempts
- Scheduler for due payments
- Events for payment lifecycle

Fraud Service:

- Kafka Streams topology
- Sliding window count by customer
- Sliding window total outgoing amount by customer
- Fraud alert events
- Rule strategy design

Reporting Service:

- Materialized account activity view
- Monthly statement job
- CSV report generation
- PDF report generation
- S3 upload through LocalStack locally

Acceptance criteria:

- Fraud alerts are produced for configured suspicious activity.
- Statements can be generated for a date range.
- Payment scheduler emits due payment events.
- Tests cover Kafka Streams topology and report generation.

