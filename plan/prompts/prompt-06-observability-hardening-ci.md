# Prompt 06: Observability, Hardening, and CI

Harden the platform for production-style operation.

Add:

- OpenTelemetry tracing
- JSON structured logs
- Correlation ID propagation across REST and Kafka
- Prometheus metrics
- Grafana dashboards
- Resilience4j retry, circuit breaker, rate limiter, and bulkhead
- Kubernetes manifests
- Helm chart
- GitHub Actions CI with unit tests, integration tests, and Docker build
- Load tests with k6 or Gatling
- ADRs explaining key architecture decisions

Acceptance criteria:

- End-to-end flow is observable in traces.
- Dashboards show request rate, error rate, latency, JVM metrics, and Kafka lag.
- CI blocks broken builds.
- Documentation explains failure modes and recovery behavior.

