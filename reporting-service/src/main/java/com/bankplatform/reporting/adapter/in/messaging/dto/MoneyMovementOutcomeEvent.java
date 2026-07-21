package com.bankplatform.reporting.adapter.in.messaging.dto;

import java.math.BigDecimal;

/** Wire shape published by Account Service on money-deposited / money-withdrawn. */
public record MoneyMovementOutcomeEvent(
        String transactionId,
        String accountId,
        String customerId,
        BigDecimal amount,
        String currency,
        String status,
        String failureReason) {}
