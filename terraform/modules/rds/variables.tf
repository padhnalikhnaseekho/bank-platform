variable "name_prefix" {
  description = "Prefix applied to resource names/tags."
  type        = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "engine_version" {
  type    = string
  default = "16.4"
}

variable "instance_class" {
  description = "db.t4g.micro is Free-Tier-eligible and enough for a dev/portfolio workload; size up for anything real."
  type        = string
  default     = "db.t4g.micro"
}

variable "allocated_storage_gb" {
  type    = number
  default = 20
}

variable "database_name" {
  description = "Matches plan/DATABASE.md: one shared Postgres instance, one schema per service, all created by each service's own Flyway migration."
  type        = string
  default     = "bank_platform"
}

variable "master_username" {
  type    = string
  default = "bank_platform_admin"
}

variable "multi_az" {
  description = "Run a synchronous standby in a second AZ for automatic failover. Doubles cost — off by default for dev."
  type        = bool
  default     = false
}

variable "backup_retention_days" {
  type    = number
  default = 7
}

variable "deletion_protection" {
  type    = bool
  default = false
}

variable "skip_final_snapshot" {
  description = "Set false for anything you'd be upset to lose — dev defaults to true so `terraform destroy` doesn't hang waiting on a snapshot."
  type        = bool
  default     = true
}

variable "tags" {
  type    = map(string)
  default = {}
}
