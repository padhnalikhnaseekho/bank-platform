# Prompt 03: Notification, Audit, Retry, and DLQ

Implement Notification Service and Audit Service.

Notification Service:

- Consume notification-requested and business events that require customer notification.
- Provide mock email, SMS, and push adapters.
- Track delivery attempts.
- Configure retry and DLQ behavior.

Audit Service:

- Consume all business topics.
- Store immutable audit records.
- Provide search API for admins.
- Archive batches to S3-compatible storage through LocalStack.

Acceptance criteria:

- Consumers are idempotent.
- Poison messages go to DLQ after configured retries.
- Audit records cannot be updated through application code.
- Integration tests verify successful consumption and DLQ path.

