variable "name_prefix" {
  description = "Prefix applied to bucket names, e.g. \"bank-platform-dev\"."
  type        = string
}

variable "buckets" {
  description = <<-EOT
    Map of logical bucket key (e.g. "statements") to its config. The actual bucket name is
    "<name_prefix>-<key>". Bucket names must be globally unique across all of AWS, which is
    why the environment name belongs in name_prefix rather than in plan/AWS.md's literal
    "bank-platform-statements" naming.
  EOT
  type = map(object({
    versioning_enabled         = bool
    noncurrent_expiration_days = optional(number)
    glacier_transition_days    = optional(number)
  }))
}

variable "tags" {
  description = "Common tags merged into every resource."
  type        = map(string)
  default     = {}
}
