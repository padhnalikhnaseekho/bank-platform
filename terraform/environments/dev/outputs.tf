output "alb_dns_name" {
  value = module.alb.dns_name
}

output "ecr_repository_urls" {
  value = module.ecr.repository_urls
}

output "rds_endpoint" {
  value = module.rds.endpoint
}

output "rds_master_user_secret_arn" {
  value = module.rds.master_user_secret_arn
}

output "msk_bootstrap_brokers" {
  value = module.msk.bootstrap_brokers_sasl_iam
}

output "s3_bucket_names" {
  value = module.s3.bucket_ids
}

output "sqs_retry_queue_urls" {
  value = module.sqs.retry_queue_urls
}

output "sqs_dead_letter_queue_url" {
  value = module.sqs.dead_letter_queue_url
}

output "sns_topic_arns" {
  value = module.sns.topic_arns
}

output "jwt_signing_key_secret_arn" {
  value = module.secrets.jwt_signing_key_secret_arn
}
