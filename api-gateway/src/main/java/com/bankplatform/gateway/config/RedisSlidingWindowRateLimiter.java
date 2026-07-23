package com.bankplatform.gateway.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * A sliding-window-log rate limiter backed by a Redis sorted set, replacing the earlier in-memory
 * Resilience4j fixed-window limiter (see docs/adr/0009-in-memory-rate-limiter.md and its successor
 * docs/adr/0012-redis-backed-rate-limiter.md). Fixed-window counters allow a client to burst up to
 * 2x the nominal limit across a window boundary; sliding-window-log tracks each request's exact
 * timestamp so the limit is enforced over any trailing window, not just fixed clock-aligned
 * buckets. Backing it with Redis (instead of an in-process map) also means the limit is shared
 * correctly across multiple gateway replicas.
 */
@Component
public class RedisSlidingWindowRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> script;
    private final long capacity;
    private final long windowMillis;

    public RedisSlidingWindowRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${bank-platform.rate-limit.capacity:100}") long capacity,
            @Value("${bank-platform.rate-limit.period-seconds:60}") long periodSeconds) {
        this.redisTemplate = redisTemplate;
        this.capacity = capacity;
        this.windowMillis = Duration.ofSeconds(periodSeconds).toMillis();
        this.script = new DefaultRedisScript<>(readScript(), Long.class);
    }

    private static String readScript() {
        try (var in =
                new ClassPathResource("scripts/sliding-window-rate-limiter.lua").getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;
        Long allowed =
                redisTemplate.execute(
                        script,
                        List.of("rate-limit:" + key),
                        String.valueOf(now),
                        String.valueOf(windowStart),
                        String.valueOf(capacity),
                        String.valueOf(windowMillis),
                        now + "-" + UUID.randomUUID());
        return allowed != null && allowed == 1L;
    }
}
