-- Sliding-window-log rate limiter over a Redis sorted set.
--
-- KEYS[1] = rate limit key (e.g. "rate-limit:<route>:<clientIp>")
-- ARGV[1] = now, in epoch millis (the score for this request's entry)
-- ARGV[2] = window start, in epoch millis (now - windowMillis)
-- ARGV[3] = capacity (max requests allowed within the window)
-- ARGV[4] = window size, in millis (used as the key's TTL so idle keys expire on their own)
-- ARGV[5] = a unique member id for this request (now alone can collide within the same
--           millisecond, and a sorted set only ever stores one entry per member)
--
-- Runs as a single EVAL so the "count then add" sequence is atomic even when many gateway
-- replicas race against the same key.
redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[2])
local count = redis.call('ZCARD', KEYS[1])
if count < tonumber(ARGV[3]) then
    redis.call('ZADD', KEYS[1], ARGV[1], ARGV[5])
    redis.call('PEXPIRE', KEYS[1], ARGV[4])
    return 1
else
    return 0
end
