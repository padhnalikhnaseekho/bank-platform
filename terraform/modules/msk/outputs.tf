output "bootstrap_brokers_sasl_iam" {
  value = aws_msk_serverless_cluster.this.bootstrap_brokers_sasl_iam
}

output "cluster_arn" {
  value = aws_msk_serverless_cluster.this.arn
}

output "security_group_id" {
  value = aws_security_group.msk.id
}
