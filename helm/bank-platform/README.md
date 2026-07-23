# bank-platform Helm chart

A portable Kubernetes alternative to the Terraform/ECS deployment in `terraform/` (see
[ADR-0006](../../docs/adr/0006-ecs-fargate-over-eks.md) for why ECS Fargate, not EKS, is the
deployment target that Terraform actually provisions). This chart is for local/demo clusters
(kind, minikube, k3d) or as a starting point if a future requirement needs the Kubernetes
ecosystem specifically.

## What this chart does not include

Postgres, Kafka, and the observability stack (Prometheus/Grafana/Jaeger) are **not** bundled —
same boundary as the Terraform, which provisions RDS/MSK/ElastiCache separately from the ECS
services. Point `values.yaml`'s `global.postgres`/`global.kafka`/`global.otlp` at whatever you
already run (a Bitnami Postgres/Kafka chart, `docker/docker-compose.yml`'s containers via
`host.docker.internal` if using a local cluster, or RDS/MSK if pointing a real cluster at AWS).

## Building images

No Dockerfiles exist in this repo — every service already has the Spring Boot Gradle plugin
applied, so images are built with Cloud Native Buildpacks via the built-in `bootBuildImage`
task, no Dockerfile needed:

```bash
./gradlew bootBuildImage
```

This produces one image per service tagged `docker.io/library/<service-name>:0.1.0-SNAPSHOT`
(module name + `version` from the root `build.gradle.kts`), which matches this chart's default
`global.imageRegistry`/`global.imageTag` values. For a local cluster, load them directly
instead of pushing to a registry:

```bash
kind load docker-image account-service:0.1.0-SNAPSHOT --name <your-cluster>
# or: minikube image load account-service:0.1.0-SNAPSHOT
```

To deploy to a real registry, retag and push each image, then override
`global.imageRegistry`/`global.imageTag` at install time.

## Installing

```bash
helm install bank-platform ./helm/bank-platform \
  --set global.postgres.host=<your-postgres-host> \
  --set global.kafka.bootstrapServers=<your-kafka-bootstrap>
```

## Layout

- `values.yaml` — one entry per service under `services:` (port, whether it needs
  DB/Kafka/JWKS env vars, replica count, per-service env overrides) plus shared `global`
  config (Postgres, Kafka, OTLP, JWKS URI, rate limiter, resource requests).
- `templates/deployment.yaml` / `templates/service.yaml` — a single generic template ranged
  over `.Values.services`, rather than one file per service, so adding a service is a
  `values.yaml` entry, not a new template.
- `templates/secret.yaml` — Postgres credentials. Override `global.postgres.username`/
  `.password` at install time (`--set` or `-f` a separate values file) rather than committing
  real credentials.
- `templates/ingress.yaml` — disabled by default (`ingress.enabled: false`); routes only to
  api-gateway, matching the ALB-fronts-only-api-gateway design in the Terraform.

## Verified

`helm lint` and `helm template` both run clean (22 rendered documents, valid YAML) — see the
per-service env vars this produces before relying on it against a real cluster, since it has
not been `helm install`'d against a live Kubernetes cluster in this environment.
