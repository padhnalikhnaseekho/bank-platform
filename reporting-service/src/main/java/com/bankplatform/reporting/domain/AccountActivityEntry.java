package com.bankplatform.reporting.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One row of the account_activity_view read model, built from Kafka events. */
public record AccountActivityEntry(
        UUID id,
        UUID customerId,
        UUID accountId,
        String eventType,
        BigDecimal amount,
        String currency,
        Instant occurredAt) {

    public static AccountActivityEntry create(
            UUID customerId,
            UUID accountId,
            String eventType,
            BigDecimal amount,
            String currency,
            Instant occurredAt) {
        return new AccountActivityEntry(
                UUID.randomUUID(), customerId, accountId, eventType, amount, currency, occurredAt);
    }
}
