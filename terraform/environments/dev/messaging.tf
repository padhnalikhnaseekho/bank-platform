module "msk" {
  source = "../../modules/msk"

  name_prefix        = local.name_prefix
  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
  tags               = local.common_tags
}

module "sqs" {
  source = "../../modules/sqs"

  name_prefix = local.name_prefix
  tags        = local.common_tags

  retry_queues = {
    "notification-retry-queue" = {
      visibility_timeout_seconds = 30
      message_retention_seconds  = 345600 # 4 days
      max_receive_count          = 5
    }
    "report-generation-retry-queue" = {
      visibility_timeout_seconds = 60
      message_retention_seconds  = 345600
      max_receive_count          = 5
    }
  }
}

module "sns" {
  source = "../../modules/sns"

  name_prefix = local.name_prefix
  tags        = local.common_tags
  topic_names = ["customer-notifications", "fraud-alerts", "operations-alerts"]
}
