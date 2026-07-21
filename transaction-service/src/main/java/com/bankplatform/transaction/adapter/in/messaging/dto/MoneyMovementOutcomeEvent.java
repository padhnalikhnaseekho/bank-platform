package com.bankplatform.transaction.adapter.in.messaging.dto;

import java.math.BigDecimal;

/** Wire shape published by Account Service on money-deposited/money-withdrawn. */
public record MoneyMovementOutcomeEvent(
        String transactionId,
        String accountId,
        BigDecimal amount,
        String currency,
        String status,
        String failureReason) {}
