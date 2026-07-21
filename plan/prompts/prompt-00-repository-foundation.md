# Prompt 00: Repository Foundation

You are implementing a production-grade Event-Driven Banking Platform.

Create a multi-module Maven repository using Java 21 and Spring Boot 3.

Modules:

- `common-library`
- `api-gateway`
- `user-service`
- `account-service`
- `transaction-service`
- `payment-service`
- `fraud-service`
- `notification-service`
- `reporting-service`
- `audit-service`

Add:

- Parent `pom.xml`
- Shared dependency versions
- Docker Compose with PostgreSQL, Kafka, Redis, LocalStack, Prometheus, Grafana, and Jaeger
- GitHub Actions workflow running `mvn clean verify`
- Basic Spring Boot app class in each service
- Actuator health endpoint in each service
- README with local startup instructions

Constraints:

- Use Java 21.
- Use Spring Boot 3.
- Keep generated code minimal but compiling.
- Do not implement business logic yet.

Acceptance criteria:

- `mvn clean verify` succeeds.
- Docker Compose starts dependencies.
- Each service can start independently.

