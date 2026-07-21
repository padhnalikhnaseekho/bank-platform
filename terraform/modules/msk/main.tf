# No ingress rules here deliberately — see the identical note in modules/rds/main.tf: this
# avoids a dependency cycle with the ECS service modules that need this SG's id. The
# environment root adds the ingress rules once both sides exist.
resource "aws_security_group" "msk" {
  name        = "${var.name_prefix}-msk-sg"
  description = "MSK Serverless (9098, SASL/IAM). Ingress rules are added by the caller once client security groups exist."
  vpc_id      = var.vpc_id

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-msk-sg"
  })
}

resource "aws_vpc_security_group_egress_rule" "allow_all" {
  security_group_id = aws_security_group.msk.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# MSK Serverless over the self-managed-Kafka-on-EKS option plan/AWS.md also allows: no
# broker sizing/patching/storage to manage, and SASL/IAM authentication means no Kafka
# credentials secret is needed (the caveat in plan/AWS.md's Secrets Manager section —
# "Kafka credentials if MSK IAM auth is not used" — doesn't apply here).
resource "aws_msk_serverless_cluster" "this" {
  cluster_name = "${var.name_prefix}-msk"

  client_authentication {
    sasl {
      iam {
        enabled = true
      }
    }
  }

  vpc_config {
    subnet_ids         = var.private_subnet_ids
    security_group_ids = [aws_security_group.msk.id]
  }

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-msk"
  })
}
