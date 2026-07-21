# Prompt 01: User and Account Services

Implement User Service and Account Service.

User Service requirements:

- Customer registration
- Login with JWT
- Refresh token
- BCrypt password hashing
- Role-based authorization
- Flyway migrations
- OpenAPI docs
- Unit and integration tests

Account Service requirements:

- Open savings/current account
- Query account by ID
- List accounts for authenticated customer
- Account statuses: PENDING, ACTIVE, FROZEN, CLOSED
- Money value object
- Ledger entries
- Optimistic locking with `@Version`
- Flyway migrations
- Unit and integration tests

Architecture:

- Use Hexagonal Architecture.
- Keep domain model independent from controllers and JPA entities where practical.
- Use application services for use cases.
- Use adapters for REST and persistence.

Acceptance criteria:

- Register, login, and open account flow works end to end.
- Tests cover validation failures and successful flows.
- OpenAPI docs expose all endpoints.

