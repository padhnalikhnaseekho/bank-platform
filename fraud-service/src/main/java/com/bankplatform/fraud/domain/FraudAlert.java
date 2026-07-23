package com.bankplatform.fraud.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudAlert(
        UUID id,
        String customerId,
        FraudAlertType type,
        long transferCount,
        BigDecimal totalAmount,
        String currency,
        Instant windowStart,
        Instant windowEnd,
        Instant triggeredAt,
        String message) {

    public static FraudAlert of(
            String customerId,
            FraudAlertType type,
            TransferWindowStats stats,
            Instant windowStart,
            Instant windowEnd,
            String message) {
        return new FraudAlert(
                UUID.randomUUID(),
                customerId,
                type,
                stats.count(),
                stats.totalAmount(),
                stats.currency(),
                windowStart,
                windowEnd,
                Instant.now(),
                message);
    }
}
