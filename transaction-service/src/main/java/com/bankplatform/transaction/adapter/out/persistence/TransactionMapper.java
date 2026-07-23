package com.bankplatform.transaction.adapter.out.persistence;

import com.bankplatform.transaction.domain.Money;
import com.bankplatform.transaction.domain.Transaction;
import com.bankplatform.transaction.domain.TransactionId;
import com.bankplatform.transaction.domain.TransactionStatus;
import com.bankplatform.transaction.domain.TransactionType;
import java.util.Currency;

final class TransactionMapper {

    private TransactionMapper() {}

    static Transaction toDomain(TransactionEntity entity) {
        Money amount = new Money(entity.getAmount(), Currency.getInstance(entity.getCurrency()));
        return new Transaction(
                TransactionId.of(entity.getId()),
                entity.getCustomerId(),
                TransactionType.valueOf(entity.getType()),
                TransactionStatus.valueOf(entity.getStatus()),
                amount,
                entity.getSourceAccountId(),
                entity.getTargetAccountId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    static TransactionEntity toEntity(Transaction transaction) {
        return new TransactionEntity(
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
