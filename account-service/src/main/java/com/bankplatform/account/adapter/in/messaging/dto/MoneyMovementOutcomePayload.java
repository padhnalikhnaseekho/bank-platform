package com.bankplatform.account.adapter.in.messaging.dto;

import java.math.BigDecimal;

/** Published as money-deposited/money-withdrawn; status carries COMPLETED or FAILED. */
public record MoneyMovementOutcomePayload(
        String transactionId,
        String accountId,
        String customerId,
        BigDecimal amount,
        String currency,
        String status,
        String failureReason) {}
