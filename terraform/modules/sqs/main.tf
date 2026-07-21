resource "aws_sqs_queue" "dead_letter" {
  name                      = "${var.name_prefix}-dead-letter-queue"
  message_retention_seconds = var.dlq_message_retention_seconds
  sqs_managed_sse_enabled   = true

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-dead-letter-queue"
  })
}

resource "aws_sqs_queue" "retry" {
  for_each = var.retry_queues

  name                       = "${var.name_prefix}-${each.key}"
  visibility_timeout_seconds = each.value.visibility_timeout_seconds
  message_retention_seconds  = each.value.message_retention_seconds
  sqs_managed_sse_enabled    = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dead_letter.arn
    maxReceiveCount     = each.value.max_receive_count
  })

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-${each.key}"
  })
}
