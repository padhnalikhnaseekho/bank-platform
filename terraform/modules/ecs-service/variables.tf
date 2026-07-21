variable "name_prefix" {
  type = string
}

variable "service_name" {
  description = "Logical service name, e.g. \"account-service\". Used to build resource names and the Cloud Map DNS name."
  type        = string
}

variable "cluster_id" {
  type = string
}

variable "cluster_name" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "service_discovery_namespace_id" {
  type = string
}

variable "image" {
  description = "Full image reference including tag, e.g. \"<account-id>.dkr.ecr.<region>.amazonaws.com/bank-platform-dev/account-service:latest\"."
  type        = string
}

variable "container_port" {
  type    = number
  default = 8080
}

variable "cpu" {
  description = "Fargate task-level vCPU units (256 = 0.25 vCPU)."
  type        = number
  default     = 256
}

variable "memory" {
  description = "Fargate task-level memory in MiB."
  type        = number
  default     = 512
}

variable "desired_count" {
  type    = number
  default = 1
}

variable "environment" {
  description = "Plain (non-secret) container environment variables."
  type        = map(string)
  default     = {}
}

variable "secrets" {
  description = "Container env var name => Secrets Manager secret ARN, injected at task launch by the execution role created in this module."
  type        = map(string)
  default     = {}
}

variable "task_role_policy_json" {
  description = "IAM policy document (JSON) granting this service's task role exactly what it needs at runtime (S3, SQS, SNS, etc). Null means no inline policy — the task role can assume nothing beyond its trust policy."
  type        = string
  default     = null
}

variable "allowed_ingress_security_group_ids" {
  description = "Security groups allowed to reach this service's container_port (typically api-gateway's SG for backend services, or the ALB's SG for api-gateway itself)."
  type        = list(string)
  default     = []
}

variable "attach_to_alb" {
  type    = bool
  default = false
}

variable "alb_target_group_arn" {
  type    = string
  default = null
}

variable "log_retention_days" {
  type    = number
  default = 14
}

variable "tags" {
  type    = map(string)
  default = {}
}
