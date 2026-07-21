package com.bankplatform.transaction.application;

import com.bankplatform.transaction.domain.Transaction;
import java.util.UUID;

public record TransactionResult(UUID transactionId, String status) {

    public static TransactionResult from(Transaction transaction) {
        return new TransactionResult(transaction.id().value(), transaction.status().name());
    }
}
