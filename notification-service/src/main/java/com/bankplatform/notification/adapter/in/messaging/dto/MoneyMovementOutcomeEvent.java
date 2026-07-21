package com.bankplatform.notification.adapter.in.messaging.dto;

import java.math.BigDecimal;

public record MoneyMovementOutcomeEvent(
        String transactionId,
        String accountId,
        String customerId,
        BigDecimal amount,
        String currency,
        String status,
        String failureReason) {}
