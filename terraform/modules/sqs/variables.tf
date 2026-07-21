variable "name_prefix" {
  description = "Prefix applied to queue names."
  type        = string
}

variable "dlq_message_retention_seconds" {
  description = "How long the shared dead-letter queue retains undeliverable messages."
  type        = number
  default     = 1209600 # 14 days, the SQS maximum
}

variable "retry_queues" {
  description = "Map of logical queue key (e.g. \"notification-retry-queue\") to its config. Each retry queue redrives to the shared dead-letter queue after max_receive_count deliveries."
  type = map(object({
    visibility_timeout_seconds = number
    message_retention_seconds  = number
    max_receive_count          = number
  }))
}

variable "tags" {
  description = "Common tags merged into every resource."
  type        = map(string)
  default     = {}
}
