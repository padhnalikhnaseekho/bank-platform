# ADR-0012: Redis-backed sliding-window-log rate limiter, replacing the in-memory one

## Status

Accepted — supersedes [ADR-0009](0009-in-memory-rate-limiter.md)

## Context

ADR-0009 documented two known limitations of the original in-memory Resilience4j rate limiter:
it behaves like a fixed-window counter (a client can burst up to ~2x the nominal limit across a
window boundary), and its state doesn't survive multiple api-gateway replicas, since each
instance would get its own independent limit. `plan/DATABASE.md`'s own "Redis Usage" section
names "API Gateway rate limits" as Redis's first intended use case — Redis has been running
unused in `docker-compose.yml` this entire time.

## Decision

`api-gateway` now rate-limits through `RedisSlidingWindowRateLimiter`, backed by a Redis sorted
set per client IP (`rate-limit:{ip}`), using the sliding-window-log algorithm: each request's
timestamp is added as a sorted-set member, entries older than the window are trimmed
(`ZREMRANGEBYSCORE`), and the request is allowed only if the remaining count is under capacity.
The whole check-and-add sequence runs as one Lua script (`EVAL`) so it's atomic even under
concurrent requests and across multiple gateway replicas — no other client's request can slip in
between the count and the add. The member added to the sorted set is `{timestamp}-{uuid}`, not
just the timestamp, since two requests can legitimately land in the same millisecond and a
sorted set only stores one entry per distinct member.

The Resilience4j `RateLimiterRegistry` bean and its dependency are removed entirely from
api-gateway — there's no reason to keep both implementations side by side once the Redis-backed
one covers the same behavior with fewer limitations.

## Consequences

- Fixes both limitations ADR-0009 called out: the sliding-window-log algorithm enforces the
  limit over any trailing window (no boundary-burst), and the limit is now correctly shared
  across however many gateway replicas are running, since they all check the same Redis key.
- Adds a real infrastructure dependency (Redis reachability) to every gateway request's hot
  path that wasn't there before — a Redis outage now means the gateway can't rate-limit at all.
  Accepted for this platform's scale; if this needed to be more fault-tolerant, the filter would
  need a documented fail-open or fail-closed decision for that case, which it does not have
  today (a failed `redisTemplate.execute` call currently propagates as a request failure).
- Verified live end-to-end: with `RATE_LIMIT_CAPACITY=3`, the first 3 registration requests
  through the real gateway succeeded and the 4th/5th got 429; inspecting the Redis sorted set
  directly afterward showed exactly 3 members, each timestamped and uniquely keyed, with a TTL
  matching the configured window.
- Tests use a plain `GenericContainer("redis:7-alpine")` rather than a dedicated Testcontainers
  Redis module, since none is officially published for the Testcontainers version already used
  elsewhere in this project (unlike Postgres/Kafka, which do have official modules).
