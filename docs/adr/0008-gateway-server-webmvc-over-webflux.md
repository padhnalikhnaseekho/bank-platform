# ADR-0008: Spring Cloud Gateway Server WebMVC over the WebFlux gateway

## Status

Accepted

## Context

Spring Cloud Gateway ships two implementations: the original reactive/WebFlux gateway, and
Spring Cloud Gateway Server MVC — a servlet-based, functional-routing implementation. Every
other service in this platform (`spring-boot-starter-web`, blocking JPA, blocking JDBC
Kafka clients) is servlet/Tomcat-based, not reactive.

## Decision

`api-gateway` uses `spring-cloud-starter-gateway-server-webmvc`
(`api-gateway/src/main/java/com/bankplatform/gateway/config/GatewayRoutesConfig.java`), with
routes defined as `RouterFunction<ServerResponse>` beans using the functional
`route(...).route(path(...), http()).before(uri(...))` style, one bean per downstream service.

## Consequences

- One programming model (blocking servlet MVC) across the entire platform, including the
  gateway — no team member needs to reason about Reactor/`Mono`/`Flux` semantics anywhere in
  this codebase.
- Security, filters, and testing all use the familiar servlet stack:
  `spring-boot-starter-security`'s standard `SecurityFilterChain`, and gateway routes are
  tested with plain `MockMvc` + WireMock stubs (`GatewayRoutingTest`, `RateLimitTest`) rather
  than `WebTestClient`.
- Trade-off accepted: the WebFlux gateway's built-in `RequestRateLimiter` and its wider
  ecosystem of reactive filters aren't available. Gateway Server MVC's own Bucket4j filter
  function requires a distributed `AsyncProxyManager` bean this deployment doesn't provision, so
  the rate limiter here is hand-rolled instead — first as an in-memory Resilience4j `RateLimiter`
  ([ADR-0009](0009-in-memory-rate-limiter.md)), later replaced with a Redis-backed
  sliding-window-log implementation ([ADR-0012](0012-redis-backed-rate-limiter.md)) once Redis
  was wired into the platform for real.
- Thread-per-request blocking I/O at the gateway is an accepted ceiling on concurrency compared
  to a reactive gateway, appropriate for this platform's scale and consistent with every
  downstream service already being blocking.
