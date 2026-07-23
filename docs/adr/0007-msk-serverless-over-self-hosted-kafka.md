# ADR-0007: MSK Serverless over self-hosted Kafka on AWS

## Status

Accepted

## Context

Local development runs a single-broker Kafka container (`docker/docker-compose.yml`), which is
fine for one developer but not something to run in production on ECS. AWS offers running
Kafka on self-managed EC2/ECS, MSK (provisioned brokers), or MSK Serverless.

## Decision

`terraform/modules/msk` provisions MSK Serverless with SASL/IAM authentication, matching the
per-service least-privilege IAM approach used everywhere else in the Terraform
(see [ADR-0006](0006-ecs-fargate-over-eks.md)).

## Consequences

- No broker count, instance type, or storage sizing to choose or later resize — serverless
  scales with actual throughput, which suits a platform whose real traffic is unknown (a
  portfolio/demo deployment, not a sized-for-production banking system).
- IAM-based auth (rather than a shared SASL/SCRAM secret) means each service's Kafka access is
  just another grant on its existing task role, consistent with how it already reaches
  Secrets Manager and S3 — no separate credential rotation story for Kafka specifically.
- Trade-off accepted: MSK Serverless has less configuration surface than provisioned MSK (no
  control over broker-level configs, storage autoscaling behavior, or reserved capacity
  pricing), which matters for cost-sensitive or highly-tuned production deployments but not for
  this one.
- Application code needed no changes for this: `spring-boot-starter-kafka`'s bootstrap-servers
  property is the only thing that differs between local (`localhost:9094`) and AWS (the MSK
  Serverless endpoint via `KAFKA_BOOTSTRAP_SERVERS`).
