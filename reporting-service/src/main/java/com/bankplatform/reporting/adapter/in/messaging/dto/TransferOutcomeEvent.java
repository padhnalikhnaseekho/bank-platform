package com.bankplatform.reporting.adapter.in.messaging.dto;

import java.math.BigDecimal;

/** Wire shape published by Account Service on transfer-completed. */
public record TransferOutcomeEvent(
        String transactionId,
        String sourceAccountId,
        String targetAccountId,
        String sourceCustomerId,
        String targetCustomerId,
        BigDecimal amount,
        String currency,
        String failureReason) {}
