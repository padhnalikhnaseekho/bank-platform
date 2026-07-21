package com.bankplatform.account.adapter.in.messaging.dto;

import java.math.BigDecimal;

/** Published as transfer-completed/transfer-failed. */
public record TransferOutcomePayload(
        String transactionId,
        String sourceAccountId,
        String targetAccountId,
        String sourceCustomerId,
        String targetCustomerId,
        BigDecimal amount,
        String currency,
        String failureReason) {}
