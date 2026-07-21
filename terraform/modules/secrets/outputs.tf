output "jwt_signing_key_secret_arn" {
  value = aws_secretsmanager_secret.jwt_signing_key.arn
}

output "provider_credential_secret_arns" {
  value = { for k, s in aws_secretsmanager_secret.provider_credentials : k => s.arn }
}
