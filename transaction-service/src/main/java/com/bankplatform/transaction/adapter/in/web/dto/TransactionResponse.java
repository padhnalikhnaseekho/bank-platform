package com.bankplatform.transaction.adapter.in.web.dto;

import com.bankplatform.transaction.domain.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID customerId,
        String type,
        String status,
        BigDecimal amount,
        String currency,
        UUID sourceAccountId,
        UUID targetAccountId,
        Instant createdAt,
        Instant updatedAt) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.id().value(),
                transaction.customerId(),
                transaction.type().name(),
                transaction.status().name(),
                transaction.amount().amount(),
                transaction.amount().currency().getCurrencyCode(),
                transaction.sourceAccountId(),
                transaction.targetAccountId(),
                transaction.createdAt(),
                transaction.updatedAt());
    }
}
