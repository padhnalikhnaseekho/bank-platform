# Ingress rules for RDS/MSK live here rather than inside their own modules, and reference
# the ECS services' security groups — see the dependency-cycle note at the top of
# services.tf and the matching comments in modules/rds, modules/msk, modules/elasticache.

locals {
  # fraud-service has no datasource; api-gateway is the only service with neither.
  db_client_security_group_ids = [
    module.user_service.security_group_id,
    module.account_service.security_group_id,
    module.transaction_service.security_group_id,
    module.payment_service.security_group_id,
    module.notification_service.security_group_id,
    module.audit_service.security_group_id,
    module.reporting_service.security_group_id,
  ]

  # Every service except api-gateway either publishes through the outbox pattern or
  # consumes Kafka topics directly.
  kafka_client_security_group_ids = concat(local.db_client_security_group_ids, [
    module.fraud_service.security_group_id,
  ])
}

resource "aws_vpc_security_group_ingress_rule" "postgres_from_services" {
  for_each = toset(local.db_client_security_group_ids)

  security_group_id            = module.rds.security_group_id
  referenced_security_group_id = each.value
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "msk_from_services" {
  for_each = toset(local.kafka_client_security_group_ids)

  security_group_id            = module.msk.security_group_id
  referenced_security_group_id = each.value
  from_port                    = 9098
  to_port                      = 9098
  ip_protocol                  = "tcp"
}

# ElastiCache has no ingress rules wired to any service: per plan/DATABASE.md's own Redis
# Usage list (rate limits, idempotency cache, fraud velocity counters, session revocation),
# nothing in the codebase actually connects to Redis yet — see the same caveat already
# tracked against the docker-compose redis service in plan/IMPLEMENTATION_CHECKLIST.md.
