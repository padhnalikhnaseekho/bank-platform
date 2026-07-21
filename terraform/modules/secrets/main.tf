terraform {
  required_providers {
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
}

# User Service currently signs JWTs (RS256) with an RSA key pair generated fresh in-memory
# on every restart (see JwtKeyConfig.java's own comment) — every restart invalidates every
# outstanding token and JWKS URI response. This provisions the persistent replacement, but
# JwtKeyConfig.java still needs a follow-up change to actually read it from here instead of
# calling KeyPairGenerator itself; provisioning the secret doesn't wire up the consumer.
resource "tls_private_key" "jwt_signing_key" {
  algorithm = "RSA"
  rsa_bits  = 2048
}

resource "aws_secretsmanager_secret" "jwt_signing_key" {
  name = "${var.name_prefix}/jwt-signing-key"

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-jwt-signing-key"
  })
}

resource "aws_secretsmanager_secret_version" "jwt_signing_key" {
  secret_id     = aws_secretsmanager_secret.jwt_signing_key.id
  secret_string = tls_private_key.jwt_signing_key.private_key_pem
}

resource "aws_secretsmanager_secret" "provider_credentials" {
  for_each = toset(var.provider_credential_secrets)

  name = "${var.name_prefix}/${each.value}"

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-${each.value}"
  })
}

resource "aws_secretsmanager_secret_version" "provider_credentials" {
  for_each = toset(var.provider_credential_secrets)

  secret_id     = aws_secretsmanager_secret.provider_credentials[each.value].id
  secret_string = jsonencode({ "placeholder" = "set-me-after-provisioning" })

  lifecycle {
    ignore_changes = [secret_string]
  }
}
