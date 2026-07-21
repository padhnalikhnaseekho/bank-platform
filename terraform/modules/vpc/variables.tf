variable "name_prefix" {
  description = "Prefix applied to all resource names/tags created by this module."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zone_count" {
  description = "Number of AZs to spread public/private subnets across."
  type        = number
  default     = 2
}

variable "single_nat_gateway" {
  description = "Use one NAT Gateway for all private subnets instead of one per AZ. Cheaper, less available — fine for a dev/portfolio environment, not for production."
  type        = bool
  default     = true
}

variable "tags" {
  description = "Common tags merged into every resource."
  type        = map(string)
  default     = {}
}
