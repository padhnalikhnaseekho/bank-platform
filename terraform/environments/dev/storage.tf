module "s3" {
  source = "../../modules/s3"

  name_prefix = local.name_prefix
  tags        = local.common_tags

  buckets = {
    statements = {
      versioning_enabled         = false
      noncurrent_expiration_days = 90
      glacier_transition_days    = null
    }
    reports = {
      versioning_enabled         = false
      noncurrent_expiration_days = 90
      glacier_transition_days    = null
    }
    audit-archive = {
      versioning_enabled         = true
      noncurrent_expiration_days = null
      glacier_transition_days    = 90
    }
  }
}
