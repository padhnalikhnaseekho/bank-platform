# Prompt 05: AWS Infrastructure

Add Terraform and deployment documentation for AWS.

Infrastructure:

- VPC with public and private subnets
- ECS Fargate or EKS deployment path
- RDS PostgreSQL
- ElastiCache Redis
- MSK or documented Kafka deployment alternative
- S3 buckets for statements, reports, and audit archive
- SQS retry and DLQ queues
- SNS topics
- Secrets Manager secrets
- CloudWatch log groups, metrics, dashboards, and alarms
- ECR repositories

Acceptance criteria:

- Terraform is modular.
- IAM policies follow least privilege.
- README explains local, staging, and production deployment.
- Service configuration supports environment variables and Secrets Manager abstraction.

