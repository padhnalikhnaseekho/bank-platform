package com.bankplatform.account.adapter.in.messaging.dto;

import java.math.BigDecimal;

/** Wire shape published by Transaction Service on transaction-created / transfer-started. */
public record TransactionCreatedEvent(
        String transactionId,
        String type,
        String sourceAccountId,
        String targetAccountId,
        BigDecimal amount,
        String currency) {}
