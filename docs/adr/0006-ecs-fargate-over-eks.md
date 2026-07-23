# ADR-0006: ECS Fargate over EKS for compute

## Status

Accepted

## Context

`plan/AWS.md` and `plan/ROADMAP.md` Phase 4 call for running 8 Spring Boot services plus
api-gateway on AWS. Both ECS Fargate and EKS are on the table (this is also one of the
`plan/INTERVIEW_QUESTIONS.md` System Design questions — "Compare ECS Fargate and EKS for this
platform").

## Decision

The Terraform in `terraform/` provisions ECS Fargate: one cluster, Cloud Map for private
service discovery, one task definition and one `ecs-service` module instance per service, and
an ALB fronting only api-gateway (`terraform/README.md`).

## Consequences

- No control plane, node group, or Kubernetes version to operate — Fargate tasks are the unit
  of compute directly, which matches a single-team, ~10-service platform better than standing
  up cluster autoscaling, node AMIs, and CNI configuration for EKS.
- Per-service IAM is simpler to reason about: each `ecs-service` module instance gets its own
  execution role (pull image, write logs, read its own secrets) and task role (only
  reporting-service's task role has a real grant today, for S3 — see `services.tf`), which maps
  directly onto ECS's task-definition-level IAM rather than needing IRSA/service-account
  wiring.
- Trade-off explicitly accepted: EKS would give access to the broader Kubernetes ecosystem
  (Helm charts, operators, HPA/VPA, service mesh) that this platform doesn't need yet. If a
  future requirement needs that ecosystem, migrating means rewriting the compute layer — the
  Terraform networking/data modules (VPC, RDS, ElastiCache, MSK, S3, SQS, SNS) are reusable
  either way since they don't depend on the compute choice.
- The parallel Helm chart under `helm/bank-platform/` (see `helm/bank-platform/README.md`)
  exists for learning/demo purposes and as a portable alternative, but ECS Fargate is the
  deployment target this Terraform actually provisions and that CI could plausibly deploy to.
