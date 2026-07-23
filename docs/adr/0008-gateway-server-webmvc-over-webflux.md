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
- Trade-off accepted: the WebFlux gateway's `RequestRateLimiter` (Redis-backed) and its wider
  ecosystem of reactive filters aren't available. This is why the api-gateway rate limiter uses
  a plain in-memory Resilience4j `RateLimiter` instead of Gateway Server MVC's own Bucket4j
  filter function, which itself requires a distributed `AsyncProxyManager` bean this deployment
  doesn't provision — see [ADR-0009](0009-in-memory-rate-limiter.md).
- Thread-per-request blocking I/O at the gateway is an accepted ceiling on concurrency compared
  to a reactive gateway, appropriate for this platform's scale and consistent with every
  downstream service already being blocking.
