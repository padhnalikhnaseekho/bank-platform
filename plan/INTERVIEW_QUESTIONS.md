# Interview Questions

## Java 21

1. Explain the difference between `record`, class, and Lombok DTO.
2. When would you use `Optional` and when would you avoid it?
3. Explain Java memory model visibility issues in concurrent balance updates.
4. How does `ConcurrentHashMap` differ from `HashMap` with synchronization?
5. Explain virtual threads and where they help in Spring Boot services.
6. What are sealed classes and how could they model transaction commands?
7. Explain equals and hashCode pitfalls in JPA entities.
8. How do streams differ from loops in performance and readability?
9. Explain checked versus unchecked exceptions in service boundaries.
10. What causes deadlocks and how can lock ordering reduce them?

## Spring Boot

1. What happens when a request enters a Spring MVC controller?
2. Explain `@Transactional` propagation and isolation.
3. Why should domain objects avoid direct dependency on Spring annotations?
4. How does Spring Security validate JWT tokens?
5. What is the difference between authentication and authorization?
6. How does Spring Data JPA implement repositories?
7. Explain lazy loading and the N+1 query problem.
8. How do profiles help local, test, and AWS deployments?
9. What does Spring Actuator expose?
10. How do you implement global exception handling?

## Kafka

1. Explain topic, partition, offset, and consumer group.
2. Why does partition key choice matter?
3. How do you make a Kafka consumer idempotent?
4. What is at-least-once delivery and how does it affect money movement?
5. How does the Outbox pattern prevent lost events?
6. When would you use Kafka Streams instead of plain consumers?
7. Explain retry topics and dead-letter topics.
8. How do you evolve event schemas safely?
9. What is consumer lag and how do you monitor it?
10. Explain exactly-once semantics and its limits.

## Database and Transactions

1. Explain optimistic versus pessimistic locking.
2. What isolation level would you use for account balance updates?
3. Why should every balance update create a ledger entry?
4. How would you design indexes for statement queries?
5. What is a transaction boundary in a Saga?
6. How do you handle duplicate external requests?
7. What causes phantom reads?
8. When would you denormalize reporting data?
9. How do database migrations work in CI/CD?
10. Why should services not share database tables?

## AWS

1. Compare ECS Fargate and EKS for this platform.
2. Why put services in private subnets?
3. How should services access Secrets Manager?
4. What CloudWatch alarms matter for a banking API?
5. When would you use SQS instead of Kafka?
6. How do S3 lifecycle policies help audit archive cost?
7. What IAM permissions does Reporting Service need?
8. How does RDS Multi-AZ improve availability?
9. What is the role of security groups?
10. How would you deploy with zero downtime?

## System Design

1. Design a reliable money transfer flow.
2. How do you prevent double debit on retry?
3. How do you detect fraud in near real time?
4. What happens if Kafka is temporarily unavailable?
5. How does the system recover from a failed notification provider?
6. What data belongs in audit logs?
7. How would you support 10x transaction volume?
8. Which operations must be strongly consistent?
9. Which operations can be eventually consistent?
10. How would you explain this architecture to a non-technical stakeholder?

