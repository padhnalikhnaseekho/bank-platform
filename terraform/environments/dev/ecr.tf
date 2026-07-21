module "ecr" {
  source = "../../modules/ecr"

  name_prefix   = local.name_prefix
  tags          = local.common_tags
  service_names = keys(local.service_ports)
}
