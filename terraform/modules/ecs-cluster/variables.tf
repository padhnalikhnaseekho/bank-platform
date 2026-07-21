variable "name_prefix" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "service_discovery_namespace" {
  description = "Private DNS namespace services resolve each other through, e.g. \"bank-platform.local\". Matches how api-gateway's application.yml already expects per-service *_URI env vars — those just point here in an AWS deployment instead of localhost."
  type        = string
  default     = "bank-platform.local"
}

variable "tags" {
  type    = map(string)
  default = {}
}
