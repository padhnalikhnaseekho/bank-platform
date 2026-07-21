package com.bankplatform.transaction.application.event;

import java.math.BigDecimal;

/** Published as transaction-created (deposit/withdrawal) or transfer-started (transfer). */
public record TransactionEventPayload(
        String transactionId,
        String type,
        String sourceAccountId,
        String targetAccountId,
        BigDecimal amount,
        String currency) {}
