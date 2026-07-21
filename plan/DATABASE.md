# Database Design

## PostgreSQL Ownership

Each service owns its own schema. Do not share tables across services.

Suggested schemas:

- `user_service`
- `account_service`
- `transaction_service`
- `payment_service`
- `notification_service`
- `reporting_service`
- `audit_service`

## User Service Tables

```sql
users(id, email, phone, full_name, status, created_at, updated_at)
credentials(id, user_id, password_hash, password_changed_at, failed_attempts)
roles(id, name)
user_roles(user_id, role_id)
refresh_tokens(id, user_id, token_hash, expires_at, revoked_at)
outbox_events(...)
```

Indexes:

- Unique index on `users.email`
- Index on `refresh_tokens.user_id`

## Account Service Tables

```sql
accounts(id, customer_id, account_number, type, status, currency, balance, version, created_at, updated_at)
ledger_entries(id, account_id, entry_type, amount, currency, reference_id, created_at)
outbox_events(...)
```

Indexes:

- Unique index on `accounts.account_number`
- Composite index on `(customer_id, status)`
- Index on `ledger_entries.account_id, created_at`

## Transaction Service Tables

```sql
transactions(id, customer_id, type, status, amount, currency, source_account_id, target_account_id, created_at, updated_at)
idempotency_records(id, idempotency_key, request_hash, response_body, status_code, created_at, expires_at)
outbox_events(...)
```

Indexes:

- Unique index on `idempotency_records.idempotency_key`
- Composite index on `(customer_id, created_at)`
- Composite index on `(status, created_at)`

## Payment Service Tables

```sql
payment_instructions(id, customer_id, source_account_id, payee_id, amount, currency, schedule_type, next_run_at, status)
payment_attempts(id, payment_instruction_id, transaction_id, status, attempted_at, failure_reason)
outbox_events(...)
```

## Reporting Service Tables

```sql
account_activity_view(id, customer_id, account_id, event_type, amount, currency, occurred_at)
statement_jobs(id, customer_id, account_id, period_start, period_end, status, file_url, created_at)
```

## Audit Service Tables

```sql
audit_events(id, event_id, event_type, aggregate_type, aggregate_id, payload, headers, occurred_at, stored_at)
```

Audit data can later move to DynamoDB or S3 archive depending on retention and query needs.

## Redis Usage

Use Redis for:

- API Gateway rate limits
- Short-lived idempotency cache
- Fraud velocity counters if not using only Kafka Streams state stores
- Session revocation cache

Do not use Redis as the source of truth for balances.

## Transaction Rules

- Money updates must be inside database transactions.
- Use optimistic locking by default.
- Use row-level locking for high-risk debit/credit flows.
- Every balance mutation creates a ledger entry.
- Never update balances from a Kafka consumer without idempotency tracking.

