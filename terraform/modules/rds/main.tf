resource "aws_db_subnet_group" "this" {
  name       = "${var.name_prefix}-db-subnets"
  subnet_ids = var.private_subnet_ids

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-db-subnets"
  })
}

# No ingress rules here deliberately — the ECS services that need to reach Postgres are
# created in the same root module that instantiates this one, and *they* need this
# security group's id (to allow-list it) at the same time this module would need *their*
# security group ids (to build the ingress rule), which is a genuine dependency cycle
# between two modules. The environment root adds the ingress rules once both sides exist.
resource "aws_security_group" "rds" {
  name        = "${var.name_prefix}-rds-sg"
  description = "Postgres (5432). Ingress rules are added by the caller once client security groups exist."
  vpc_id      = var.vpc_id

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-rds-sg"
  })
}

resource "aws_vpc_security_group_egress_rule" "allow_all" {
  security_group_id = aws_security_group.rds.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# manage_master_user_password lets RDS generate and rotate the password itself, storing it
# in a Secrets Manager secret it creates and owns — the app reads that secret's ARN
# (exposed via this module's output) instead of Terraform ever holding the password in
# state or requiring a separately-managed random_password resource.
resource "aws_db_instance" "this" {
  identifier     = "${var.name_prefix}-postgres"
  engine         = "postgres"
  engine_version = var.engine_version
  instance_class = var.instance_class

  allocated_storage     = var.allocated_storage_gb
  max_allocated_storage = var.allocated_storage_gb * 5
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name                     = var.database_name
  username                    = var.master_username
  manage_master_user_password = true

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  multi_az                = var.multi_az
  backup_retention_period = var.backup_retention_days

  deletion_protection       = var.deletion_protection
  skip_final_snapshot       = var.skip_final_snapshot
  final_snapshot_identifier = var.skip_final_snapshot ? null : "${var.name_prefix}-postgres-final"

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-postgres"
  })
}
