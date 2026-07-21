locals {
  name_prefix = "bank-platform-${var.environment}"

  common_tags = {
    Project     = "bank-platform"
    Environment = var.environment
    ManagedBy   = "terraform"
  }

  # Matches gradle/libs.versions.toml's port assignments (see each service's application.yml).
  service_ports = {
    api-gateway          = 8080
    user-service         = 8081
    account-service      = 8082
    transaction-service  = 8083
    payment-service      = 8084
    fraud-service        = 8085
    notification-service = 8086
    reporting-service    = 8087
    audit-service        = 8088
  }
}
