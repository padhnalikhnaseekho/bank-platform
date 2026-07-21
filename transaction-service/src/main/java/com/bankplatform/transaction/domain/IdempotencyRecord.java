package com.bankplatform.transaction.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record IdempotencyRecord(
        UUID id,
        String idempotencyKey,
        String requestHash,
        String responseBody,
        int statusCode,
        Instant createdAt,
        Instant expiresAt) {

    public static IdempotencyRecord create(String idempotencyKey, String requestHash, String responseBody,
            int statusCode) {
        Instant now = Instant.now();
        return new IdempotencyRecord(UUID.randomUUID(), idempotencyKey, requestHash, responseBody, statusCode, now,
                now.plus(Duration.ofDays(1)));
    }
}
