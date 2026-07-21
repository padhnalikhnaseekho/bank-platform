output "dns_name" {
  value = aws_lb.this.dns_name
}

output "security_group_id" {
  value = aws_security_group.alb.id
}

output "api_gateway_target_group_arn" {
  value = aws_lb_target_group.api_gateway.arn
}

output "listener_arn" {
  value = var.certificate_arn != null ? aws_lb_listener.https[0].arn : aws_lb_listener.http.arn
}
