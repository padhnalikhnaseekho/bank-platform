locals {
  service_discovery_namespace = "bank-platform.local"

  # Pure string interpolation, not a reference to any ecs-service module's own output — see
  # the note in services.tf about why that distinction is what keeps this module graph
  # acyclic. Every service resolves every other service through exactly this convention.
  service_internal_url = {
    for name, port in local.service_ports :
    name => "http://${name}.${local.service_discovery_namespace}:${port}"
  }

  jdbc_url              = "jdbc:postgresql://${module.rds.address}:${module.rds.port}/${module.rds.database_name}"
  user_service_jwks_uri = "${local.service_internal_url["user-service"]}/.well-known/jwks.json"

  db_secrets = {
    DB_USERNAME = "${module.rds.master_user_secret_arn}:username::"
    DB_PASSWORD = "${module.rds.master_user_secret_arn}:password::"
  }
}
