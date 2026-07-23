# ADR-0009: In-memory Resilience4j rate limiter at the gateway, not a distributed one

## Status

Superseded by [ADR-0012](0012-redis-backed-rate-limiter.md) — the "Consequences" section below
called out the exact two limitations (fixed-window bursting, no cross-replica sharing) that
ADR-0012 fixes by moving to a Redis-backed sliding-window-log limiter. Kept here for the
historical record of why the in-memory approach was chosen first and what specifically was
learned from it.

## Context

Phase 5 needed a rate limiter somewhere in the platform. A codebase-wide look for a natural
Resilience4j retry/circuit-breaker target came up essentially empty: there are no custom
synchronous inter-service HTTP calls in this codebase (only Spring Security's transparent JWKS
fetch), so a rate limiter on api-gateway's public routes was the one place this was clearly
worth doing regardless of internal call patterns.

Spring Cloud Gateway Server MVC (see [ADR-0008](0008-gateway-server-webmvc-over-webflux.md))
ships a built-in `Bucket4jFilterFunctions.rateLimit(...)` filter. Wiring it up threw
`NoSuchBeanDefinitionException: No qualifying bean of type
'io.github.bucket4j.distributed.proxy.AsyncProxyManager<?>'` at request time — that filter
assumes a distributed backing store (Redis, Hazelcast, etc.) that this deployment doesn't run.

## Decision

`GatewayRoutesConfig` defines its own `RateLimiterRegistry` bean (plain
`io.github.resilience4j:resilience4j-ratelimiter`, no Spring Cloud Circuit Breaker starter
needed) and a `HandlerFilterFunction` that looks up a `RateLimiter` keyed by client IP per
route, applied to every route via `.filter(...)`. Capacity and refresh period are configurable
(`bank-platform.rate-limit.capacity` / `.period-seconds`, defaults 100 req/60s).

## Consequences

- No extra infrastructure (Redis/Hazelcast) needed just to rate-limit the gateway — a real
  simplification for a single-instance gateway deployment.
- Because the limiter's state is in-process, running api-gateway as more than one replica gives
  each replica its own independent limit rather than one shared limit across the fleet — a
  correctness gap that would need a distributed store (reintroducing the Bucket4j path, with
  Redis, or a Resilience4j-compatible distributed backend) if api-gateway is ever horizontally
  scaled behind a load balancer.
- Verified live with k6 (`load-tests/money-movement.js`): 429s appear under load at the default
  limit; raising `RATE_LIMIT_CAPACITY` for a load-test run removes them, confirming the limiter
  (not a downstream service) was the source of the 429s.
