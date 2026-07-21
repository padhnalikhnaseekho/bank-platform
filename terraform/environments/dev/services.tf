# Dependency note: every cross-service reference below is either (a) a pure string built
# from local.service_internal_url / local.service_ports — never an actual module output —
# or (b) a one-directional reference to another service's security_group_id for an ingress
# rule. Never (b) in both directions between the same two services, and never (a) via a
# module output: that combination is what would create a cycle in the module graph (see the
# identical note on modules/rds, modules/elasticache, modules/msk). api-gateway is declared
# first because every JWT-verifying backend service needs to allow-list its security group;
# user-service is declared last because it in turn needs to allow-list all of theirs (they
# all fetch its JWKS document directly, not through the gateway).

module "api_gateway_service" {
  source = "../../modules/ecs-service"

  name_prefix                        = local.name_prefix
  service_name                       = "api-gateway"
  cluster_id                         = module.ecs_cluster.cluster_id
  cluster_name                       = module.ecs_cluster.cluster_name
  vpc_id                             = module.vpc.vpc_id
  private_subnet_ids                 = module.vpc.private_subnet_ids
  service_discovery_namespace_id     = module.ecs_cluster.service_discovery_namespace_id
  image                              = "${module.ecr.repository_urls["api-gateway"]}:${var.app_image_tag}"
  container_port                     = local.service_ports["api-gateway"]
  attach_to_alb                      = true
  alb_target_group_arn               = module.alb.api_gateway_target_group_arn
  allowed_ingress_security_group_ids = [module.alb.security_group_id]
  tags                               = local.common_tags

  environment = {
    USER_SERVICE_JWKS_URI   = local.user_service_jwks_uri
    USER_SERVICE_URI        = local.service_internal_url["user-service"]
    ACCOUNT_SERVICE_URI     = local.service_internal_url["account-service"]
    TRANSACTION_SERVICE_URI = local.service_internal_url["transaction-service"]
    AUDIT_SERVICE_URI       = local.service_internal_url["audit-service"]
    PAYMENT_SERVICE_URI     = local.service_internal_url["payment-service"]
    REPORTING_SERVICE_URI   = local.service_internal_url["reporting-service"]
  }
}

module "account_service" {
  source = "../../modules/ecs-service"

  name_prefix                        = local.name_prefix
  service_name                       = "account-service"
  cluster_id                         = module.ecs_cluster.cluster_id
  cluster_name                       = module.ecs_cluster.cluster_name
  vpc_id                             = module.vpc.vpc_id
  private_subnet_ids                 = module.vpc.private_subnet_ids
  service_discovery_namespace_id     = module.ecs_cluster.service_discovery_namespace_id
  image                              = "${module.ecr.repository_urls["account-service"]}:${var.app_image_tag}"
  container_port                     = local.service_ports["account-service"]
  allowed_ingress_security_group_ids = [module.api_gateway_service.security_group_id]
  tags                               = local.common_tags

  environment = {
    DB_URL                  = local.jdbc_url
    USER_SERVICE_JWKS_URI   = local.user_service_jwks_uri
    KAFKA_BOOTSTRAP_SERVERS = module.msk.bootstrap_brokers_sasl_iam
  }
  secrets = local.db_secrets
}

module "transaction_service" {
  source = "../../modules/ecs-service"

  name_prefix                        = local.name_prefix
  service_name                       = "transaction-service"
  cluster_id                         = module.ecs_cluster.cluster_id
  cluster_name                       = module.ecs_cluster.cluster_name
  vpc_id                             = module.vpc.vpc_id
  private_subnet_ids                 = module.vpc.private_subnet_ids
  service_discovery_namespace_id     = module.ecs_cluster.service_discovery_namespace_id
  image                              = "${module.ecr.repository_urls["transaction-service"]}:${var.app_image_tag}"
  container_port                     = local.service_ports["transaction-service"]
  allowed_ingress_security_group_ids = [module.api_gateway_service.security_group_id]
  tags                               = local.common_tags

  environment = {
    DB_URL                  = local.jdbc_url
    USER_SERVICE_JWKS_URI   = local.user_service_jwks_uri
    KAFKA_BOOTSTRAP_SERVERS = module.msk.bootstrap_brokers_sasl_iam
  }
  secrets = local.db_secrets
}

module "payment_service" {
  source = "../../modules/ecs-service"

  name_prefix                        = local.name_prefix
  service_name                       = "payment-service"
  cluster_id                         = module.ecs_cluster.cluster_id
  cluster_name                       = module.ecs_cluster.cluster_name
  vpc_id                             = module.vpc.vpc_id
  private_subnet_ids                 = module.vpc.private_subnet_ids
  service_discovery_namespace_id     = module.ecs_cluster.service_discovery_namespace_id
  image                              = "${module.ecr.repository_urls["payment-service"]}:${var.app_image_tag}"
  container_port                     = local.service_ports["payment-service"]
  allowed_ingress_security_group_ids = [module.api_gateway_service.security_group_id]
  tags                               = local.common_tags

  environment = {
    DB_URL                  = local.jdbc_url
    USER_SERVICE_JWKS_URI   = local.user_service_jwks_uri
    KAFKA_BOOTSTRAP_SERVERS = module.msk.bootstrap_brokers_sasl_iam
  }
  secrets = local.db_secrets
}

module "audit_service" {
  source = "../../modules/ecs-service"

  name_prefix                        = local.name_prefix
  service_name                       = "audit-service"
  cluster_id                         = module.ecs_cluster.cluster_id
  cluster_name                       = module.ecs_cluster.cluster_name
  vpc_id                             = module.vpc.vpc_id
  private_subnet_ids                 = module.vpc.private_subnet_ids
  service_discovery_namespace_id     = module.ecs_cluster.service_discovery_namespace_id
  image                              = "${module.ecr.repository_urls["audit-service"]}:${var.app_image_tag}"
  container_port                     = local.service_ports["audit-service"]
  allowed_ingress_security_group_ids = [module.api_gateway_service.security_group_id]
  tags                               = local.common_tags

  environment = {
    DB_URL                  = local.jdbc_url
    USER_SERVICE_JWKS_URI   = local.user_service_jwks_uri
    KAFKA_BOOTSTRAP_SERVERS = module.msk.bootstrap_brokers_sasl_iam
  }
  secrets = local.db_secrets
}

module "reporting_service" {
  source = "../../modules/ecs-service"

  name_prefix                        = local.name_prefix
  service_name                       = "reporting-service"
  cluster_id                         = module.ecs_cluster.cluster_id
  cluster_name                       = module.ecs_cluster.cluster_name
  vpc_id                             = module.vpc.vpc_id
  private_subnet_ids                 = module.vpc.private_subnet_ids
  service_discovery_namespace_id     = module.ecs_cluster.service_discovery_namespace_id
  image                              = "${module.ecr.repository_urls["reporting-service"]}:${var.app_image_tag}"
  container_port                     = local.service_ports["reporting-service"]
  allowed_ingress_security_group_ids = [module.api_gateway_service.security_group_id]
  task_role_policy_json              = data.aws_iam_policy_document.reporting_service_task.json
  tags                               = local.common_tags

  environment = {
    DB_URL                  = local.jdbc_url
    USER_SERVICE_JWKS_URI   = local.user_service_jwks_uri
    KAFKA_BOOTSTRAP_SERVERS = module.msk.bootstrap_brokers_sasl_iam
    # Reporting Service's S3ClientConfig only targets one configured bucket today (see
    # S3ReportStorage) — pointed at "statements" to match its current single-bucket usage.
    # The "reports" bucket is provisioned but unused until that config is split in two.
    REPORTS_S3_BUCKET = module.s3.bucket_ids["statements"]
    REPORTS_S3_REGION = var.aws_region
  }
  secrets = local.db_secrets
}

# Reporting Service is the only service with a real S3 integration today (S3ReportStorage) —
# every other service's task role is left with no inline policy, matching what their
# application code actually touches.
data "aws_iam_policy_document" "reporting_service_task" {
  statement {
    actions = ["s3:PutObject", "s3:GetObject"]
    resources = [
      "${module.s3.bucket_arns["statements"]}/*",
    ]
  }
}

module "notification_service" {
  source = "../../modules/ecs-service"

  name_prefix                    = local.name_prefix
  service_name                   = "notification-service"
  cluster_id                     = module.ecs_cluster.cluster_id
  cluster_name                   = module.ecs_cluster.cluster_name
  vpc_id                         = module.vpc.vpc_id
  private_subnet_ids             = module.vpc.private_subnet_ids
  service_discovery_namespace_id = module.ecs_cluster.service_discovery_namespace_id
  image                          = "${module.ecr.repository_urls["notification-service"]}:${var.app_image_tag}"
  container_port                 = local.service_ports["notification-service"]
  # Not reachable from api-gateway: notification-service has no REST API of its own, it
  # only consumes Kafka topics (see NotificationEventListener) — matches GatewayRoutesConfig,
  # which has no route for it.
  allowed_ingress_security_group_ids = []
  tags                               = local.common_tags

  environment = {
    DB_URL                  = local.jdbc_url
    KAFKA_BOOTSTRAP_SERVERS = module.msk.bootstrap_brokers_sasl_iam
  }
  secrets = local.db_secrets
}

module "fraud_service" {
  source = "../../modules/ecs-service"

  name_prefix                    = local.name_prefix
  service_name                   = "fraud-service"
  cluster_id                     = module.ecs_cluster.cluster_id
  cluster_name                   = module.ecs_cluster.cluster_name
  vpc_id                         = module.vpc.vpc_id
  private_subnet_ids             = module.vpc.private_subnet_ids
  service_discovery_namespace_id = module.ecs_cluster.service_discovery_namespace_id
  image                          = "${module.ecr.repository_urls["fraud-service"]}:${var.app_image_tag}"
  container_port                 = local.service_ports["fraud-service"]
  # No JDBC datasource (Kafka Streams keeps its window state in local RocksDB) and no REST
  # API called by any other service — matches fraud-service's actual dependencies.
  allowed_ingress_security_group_ids = []
  tags                               = local.common_tags

  environment = {
    KAFKA_BOOTSTRAP_SERVERS = module.msk.bootstrap_brokers_sasl_iam
  }
}

module "user_service" {
  source = "../../modules/ecs-service"

  name_prefix                    = local.name_prefix
  service_name                   = "user-service"
  cluster_id                     = module.ecs_cluster.cluster_id
  cluster_name                   = module.ecs_cluster.cluster_name
  vpc_id                         = module.vpc.vpc_id
  private_subnet_ids             = module.vpc.private_subnet_ids
  service_discovery_namespace_id = module.ecs_cluster.service_discovery_namespace_id
  image                          = "${module.ecr.repository_urls["user-service"]}:${var.app_image_tag}"
  container_port                 = local.service_ports["user-service"]
  tags                           = local.common_tags

  # Every JWT-verifying service fetches this JWKS document directly (see
  # JwtAuthenticationAutoConfiguration in common-library), not through api-gateway.
  allowed_ingress_security_group_ids = [
    module.api_gateway_service.security_group_id,
    module.account_service.security_group_id,
    module.transaction_service.security_group_id,
    module.payment_service.security_group_id,
    module.audit_service.security_group_id,
    module.reporting_service.security_group_id,
  ]

  environment = {
    DB_URL                  = local.jdbc_url
    KAFKA_BOOTSTRAP_SERVERS = module.msk.bootstrap_brokers_sasl_iam
  }
  secrets = local.db_secrets
}
