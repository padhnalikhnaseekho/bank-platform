output "topic_arns" {
  value = { for k, t in aws_sns_topic.this : k => t.arn }
}
