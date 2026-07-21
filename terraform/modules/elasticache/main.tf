resource "aws_elasticache_subnet_group" "this" {
  name       = "${var.name_prefix}-redis-subnets"
  subnet_ids = var.private_subnet_ids
}

# No ingress rules here deliberately — see the identical note in modules/rds/main.tf: this
# avoids a dependency cycle with the ECS service modules that need this SG's id. The
# environment root adds the ingress rules once both sides exist.
resource "aws_security_group" "redis" {
  name        = "${var.name_prefix}-redis-sg"
  description = "Redis (6379). Ingress rules are added by the caller once client security groups exist."
  vpc_id      = var.vpc_id

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-redis-sg"
  })
}

resource "aws_vpc_security_group_egress_rule" "allow_all" {
  security_group_id = aws_security_group.redis.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# Single node, no automatic failover — matches plan/DATABASE.md's stated usage (rate limits,
# idempotency cache, fraud velocity counters, session revocation), none of which are the
# system of record. A replication group would be the upgrade path once something durable
# depends on Redis.
resource "aws_elasticache_cluster" "this" {
  cluster_id         = "${var.name_prefix}-redis"
  engine             = "redis"
  engine_version     = var.engine_version
  node_type          = var.node_type
  num_cache_nodes    = 1
  port               = 6379
  subnet_group_name  = aws_elasticache_subnet_group.this.name
  security_group_ids = [aws_security_group.redis.id]

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-redis"
  })
}
