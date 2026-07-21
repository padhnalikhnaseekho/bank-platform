package com.bankplatform.notification.adapter.in.messaging.dto;

import java.math.BigDecimal;

public record TransferOutcomeEvent(
        String transactionId,
        String sourceAccountId,
        String targetAccountId,
        String sourceCustomerId,
        String targetCustomerId,
        BigDecimal amount,
        String currency,
        String failureReason) {}
