# Terraform: AWS Infrastructure

Implements the target architecture in `plan/AWS.md` as a deployable Terraform
configuration: VPC, RDS Postgres, ElastiCache Redis, MSK Serverless (Kafka), S3, SQS, SNS,
Secrets Manager, ECR, and an ECS Fargate deployment for every service — one environment
(`dev`) to start.

## Layout

```
terraform/
  modules/            reusable building blocks, each provisioning one concern
    vpc/              VPC, public/private subnets, IGW, NAT, route tables
    rds/               Postgres (one shared instance — matches plan/DATABASE.md's
                       one-schema-per-service design, same as the app's local docker-compose)
    elasticache/        Redis (provisioned per plan/AWS.md; unused by app code today —
                       see Known Simplifications)
    msk/                Kafka via MSK Serverless, SASL/IAM auth (no broker sizing, no
                       Kafka credentials secret needed)
    s3/                 statements / reports / audit-archive buckets — encrypted,
                       versioned where it matters, public access blocked
    sqs/                retry queues + one shared dead-letter queue with redrive policies
    sns/                customer-notifications / fraud-alerts / operations-alerts topics
    secrets/            JWT signing key (RSA keypair via the tls provider) + provider
                       credential placeholders
    ecr/                one repository per deployable service
    ecs-cluster/        the Fargate cluster + a private Cloud Map DNS namespace
    ecs-service/        reusable per-service Fargate service: task def, its own execution
                       role (secrets access) and task role (runtime AWS permissions),
                       security group, log group, Cloud Map registration
    alb/                the one public load balancer, fronting api-gateway only
  environments/
    dev/                wires every module together for one environment
```

## Design choices worth knowing about

**One RDS instance, not one per service.** The app already runs this way locally
(`docker-compose`'s single Postgres container, `create-schemas: true` per service) — this
mirrors that instead of inventing a different production topology.

**Cloud Map for internal service-to-service calls, ALB only for api-gateway.**
Every backend service registers as `<service-name>.bank-platform.local`. api-gateway's
`ACCOUNT_SERVICE_URI` etc. env vars (already read by `GatewayRoutesConfig`) point at these
DNS names in this deployment instead of `localhost`. Only api-gateway sits behind the ALB —
matches how the app is actually structured (it's the only service other services and the
public reach through).

**Per-service IAM, not one shared role.** Each `ecs-service` instance gets its own execution
role (can only read the exact secrets that service was given) and its own task role (can
only call the exact AWS APIs passed in via `task_role_policy_json`). Right now only
`reporting-service` gets a real task policy (S3 read/write on its statements bucket) because
that's the only service with a real AWS SDK integration in application code — see
`services.tf`'s comments for why the others are intentionally left with none.

**Module dependency direction.** `modules/rds`, `modules/elasticache`, and `modules/msk`
deliberately create their security groups with *no* ingress rules baked in. If they took
"allowed security group ids" as a module input, and the ECS services in turn needed the
database's connection details as an input, the two modules would depend on each other's
outputs — a real cycle Terraform can't resolve. The environment root adds those ingress
rules itself once every ECS service's security group already exists (`network-rules.tf`).
The same reasoning is why cross-service URLs (`ACCOUNT_SERVICE_URI`, the JWKS URI, etc.) are
built from plain string interpolation against a known naming convention
(`service-discovery.tf`) rather than by referencing another `ecs-service` module's output.

## Known simplifications (dev/portfolio scope, not production)

- Single NAT Gateway (not one per AZ) — cheaper, less available.
- RDS Multi-AZ off by default (`var.multi_az`).
- ElastiCache Redis is provisioned but nothing in the app connects to it yet — matches the
  same caveat already tracked against the docker-compose `redis` service in
  `plan/IMPLEMENTATION_CHECKLIST.md`. Wire it up before relying on it for rate
  limiting/idempotency/fraud counters as `plan/DATABASE.md` describes.
- MSK auth is SASL/IAM, but no service's Spring Kafka config currently includes the
  `aws-msk-iam-auth` client library or the corresponding `sasl.jaas.config` — the app talks
  plaintext to `localhost:9094` today. Deploying against this MSK cluster needs that wired up
  first.
- User Service signs JWTs with an RSA key pair generated fresh on every restart
  (`JwtKeyConfig.java`'s own comment says as much). `modules/secrets` provisions the
  persistent replacement, but `JwtKeyConfig.java` needs a follow-up code change to actually
  read from Secrets Manager instead of calling `KeyPairGenerator` itself — provisioning the
  secret doesn't wire up the consumer.
- No autoscaling, no WAF, no CloudWatch alarms/dashboards yet (`plan/AWS.md`'s CloudWatch
  section — Phase 5 territory).
- `skip_final_snapshot = true` and `deletion_protection = false` by default — flip both
  before this is anything you'd be upset to lose.

## Using this

Requires Terraform >= 1.7 and the AWS provider downloads network access (no AWS credentials
needed just to `init`/`validate`/`fmt` — those succeed against the public provider registry
alone). Actually provisioning anything needs real AWS credentials this repo has never been
run against; nothing here has been `apply`'d.

```bash
cd terraform/environments/dev
terraform init
terraform validate
terraform plan    # requires AWS credentials
```

The default backend is local state (see the commented S3 backend block in `versions.tf`) —
switch to a real backend with state locking before more than one person touches this.

Build and push images to the ECR repositories this creates
(`terraform output ecr_repository_urls`), then deploy with `app_image_tag` set to the tag you
pushed.
