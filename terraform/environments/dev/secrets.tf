module "secrets" {
  source = "../../modules/secrets"

  name_prefix = local.name_prefix
  tags        = local.common_tags

  # Provisioned per plan/AWS.md's target design, ready for when Notification Service's
  # channel adapters move off their current mock implementations onto real providers.
  provider_credential_secrets = ["email-provider", "sms-provider"]
}
