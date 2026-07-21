package com.bankplatform.notification.adapter.in.messaging.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Wire shape published by Fraud Service on fraud-alert. */
public record FraudAlertEvent(
        UUID id,
        String customerId,
        String type,
        long transferCount,
        BigDecimal totalAmount,
        String currency,
        Instant windowStart,
        Instant windowEnd,
        Instant triggeredAt,
        String message) {}
