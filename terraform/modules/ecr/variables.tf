variable "name_prefix" {
  type = string
}

variable "service_names" {
  description = "One ECR repository per deployable service, e.g. [\"api-gateway\", \"user-service\", ...]."
  type        = list(string)
}

variable "untagged_image_expiry_days" {
  type    = number
  default = 14
}

variable "tags" {
  type    = map(string)
  default = {}
}
