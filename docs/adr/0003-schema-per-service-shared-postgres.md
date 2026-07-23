# ADR-0003: One schema per service in a shared PostgreSQL instance

## Status

Accepted

## Context

The platform has 6 services with persistent state (user, account, transaction, payment,
reporting, audit). A microservices-by-the-book approach gives each its own database instance;
running 6 separate Postgres instances is real operational and cost overhead for what is a
portfolio-scale deployment (Terraform provisions one RDS instance, see `terraform/README.md`).

## Decision

Each service gets its own schema (`user_service`, `account_service`, `transaction_service`,
`payment_service`, `reporting_service`, `audit_service`) inside one shared RDS Postgres
instance, with Flyway configured per service to manage only its own schema
(`spring.flyway.schemas` / `create-schemas: true`). No service is permitted to query or join
across another service's schema — cross-service data needs go through Kafka events or a
service's own HTTP API, never a shared table.

## Consequences

- One RDS instance to provision, patch, back up, and monitor instead of 6 — matches the actual
  scale of this project.
- Schema isolation is enforced by convention and code review, not by the database — a bug
  could technically issue a cross-schema query with the same credentials. Acceptable here since
  a single team controls all services; would need per-schema database roles before this
  pattern scaled to independent teams.
- Because they share one instance, all 6 services share failure and maintenance windows for
  the database — a real availability coupling accepted for this project's scale.
- Migrating any one service to a fully separate database later only requires a schema dump/
  restore and a connection-string change, since no service ever depended on another's tables.
