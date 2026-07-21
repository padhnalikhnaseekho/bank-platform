# Services

## Common Library

Responsibilities:

- Error model and exception mapping
- Event envelope
- Idempotency abstractions
- Correlation ID filter
- OpenTelemetry propagation helpers
- Testcontainers base classes
- Shared validation annotations

Avoid sharing JPA entities or domain aggregates through this module.

## User Service

Responsibilities:

- Customer registration
- Login and JWT issue
- Refresh tokens
- Role-based authorization
- User profile management

Domain model:

- `User`
- `Credential`
- `Role`
- `RefreshToken`

Ports:

- `UserRepository`
- `PasswordHasher`
- `TokenIssuer`
- `EventPublisher`

Events:

- `user-created`
- `user-updated`
- `user-login-succeeded`
- `user-login-failed`

## Account Service

Responsibilities:

- Open bank account
- Maintain account status
- Maintain balance and ledger
- Apply debit and credit commands
- Enforce balance invariants

Domain model:

- `Account`
- `Money`
- `LedgerEntry`
- `AccountStatus`

Important rules:

- Balance cannot go below overdraft limit.
- Account must be active for debit.
- Account version is used for optimistic locking.
- High-contention balance updates can use pessimistic locking.

Events:

- `account-created`
- `account-activated`
- `account-frozen`
- `account-closed`
- `money-deposited`
- `money-withdrawn`
- `transfer-balance-applied`

## Transaction Service

Responsibilities:

- Accept deposit, withdrawal, and transfer commands
- Enforce idempotency
- Track transaction lifecycle
- Start transfer Saga

Domain model:

- `Transaction`
- `Transfer`
- `IdempotencyRecord`
- `TransactionStatus`

Statuses:

- `RECEIVED`
- `VALIDATED`
- `PROCESSING`
- `COMPLETED`
- `FAILED`
- `REVERSED`

Events:

- `transfer-started`
- `transfer-completed`
- `transfer-failed`
- `transaction-created`
- `transaction-status-changed`

## Payment Service

Responsibilities:

- Schedule future payments
- Manage recurring payment instructions
- Execute due payments
- Track attempts and failures

Domain model:

- `PaymentInstruction`
- `PaymentSchedule`
- `PaymentAttempt`

Events:

- `payment-created`
- `payment-due`
- `payment-success`
- `payment-failed`
- `payment-cancelled`

## Fraud Service

Responsibilities:

- Consume transaction events
- Apply rule strategies
- Use Kafka Streams for sliding windows
- Produce fraud alerts

Example rules:

- More than 5 transfers in 10 minutes
- Total outgoing value over 50000 INR in 10 minutes
- Transfer to new payee immediately after password reset
- Repeated failed login followed by high-value transfer

Events:

- `fraud-alert`
- `risk-score-updated`

## Notification Service

Responsibilities:

- Consume notification requests
- Send email, SMS, or push through mock providers locally
- Integrate with SNS in AWS mode
- Track delivery attempts

Events:

- `notification-requested`
- `notification-sent`
- `notification-failed`

## Reporting Service

Responsibilities:

- Build read models from Kafka events
- Generate monthly account statements
- Produce CSV and PDF reports
- Upload reports to S3

Domain model:

- `Statement`
- `ReportJob`
- `ReportFile`

## Audit Service

Responsibilities:

- Subscribe to all business topics
- Store immutable audit records
- Archive event batches to S3
- Provide compliance search APIs

Rules:

- Audit records are append-only.
- Events include schema version and producer metadata.
- Personally identifiable information is minimized or masked where possible.

## API Gateway

Responsibilities:

- Route requests to services
- Validate JWT
- Apply rate limits
- Add correlation IDs
- Expose unified OpenAPI links

