variable "name_prefix" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "health_check_path" {
  type    = string
  default = "/actuator/health"
}

variable "certificate_arn" {
  description = "ACM certificate ARN. If set, adds an HTTPS listener and redirects HTTP to it. If null, serves plain HTTP on port 80 — fine for a dev/portfolio environment, never for production."
  type        = string
  default     = null
}

variable "tags" {
  type    = map(string)
  default = {}
}
