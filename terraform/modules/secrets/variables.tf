variable "name_prefix" {
  type = string
}

variable "provider_credential_secrets" {
  description = <<-EOT
    Logical secret keys for third-party provider credentials (e.g. "email-provider",
    "sms-provider"). Terraform only creates each secret as an empty placeholder — the real
    values get filled in out-of-band (console/CLI) after provisioning, since Terraform state
    is the wrong place to hold live API keys. lifecycle.ignore_changes on the secret version
    keeps a later `terraform apply` from stomping whatever a human put there.
  EOT
  type        = list(string)
  default     = []
}

variable "tags" {
  type    = map(string)
  default = {}
}
