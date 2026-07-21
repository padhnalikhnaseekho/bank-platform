variable "name_prefix" {
  description = "Prefix applied to topic names."
  type        = string
}

variable "topic_names" {
  description = "Logical topic names, e.g. [\"customer-notifications\", \"fraud-alerts\", \"operations-alerts\"]."
  type        = list(string)
}

variable "tags" {
  description = "Common tags merged into every resource."
  type        = map(string)
  default     = {}
}
