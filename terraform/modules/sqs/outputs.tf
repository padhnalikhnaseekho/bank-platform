output "dead_letter_queue_arn" {
  value = aws_sqs_queue.dead_letter.arn
}

output "dead_letter_queue_url" {
  value = aws_sqs_queue.dead_letter.url
}

output "retry_queue_arns" {
  value = { for k, q in aws_sqs_queue.retry : k => q.arn }
}

output "retry_queue_urls" {
  value = { for k, q in aws_sqs_queue.retry : k => q.url }
}
