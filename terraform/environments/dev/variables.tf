variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "app_image_tag" {
  description = "Image tag to deploy for every service, e.g. a CI-built commit SHA or \"latest\"."
  type        = string
  default     = "latest"
}
