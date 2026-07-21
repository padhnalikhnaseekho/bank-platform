module "ecs_cluster" {
  source = "../../modules/ecs-cluster"

  name_prefix                 = local.name_prefix
  vpc_id                      = module.vpc.vpc_id
  service_discovery_namespace = local.service_discovery_namespace
  tags                        = local.common_tags
}
