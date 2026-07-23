package com.bankplatform.transaction.domain;

import com.bankplatform.common.error.ConflictException;
import java.time.Instant;
import java.util.UUID;

public class Transaction {

    private final TransactionId id;
    private final UUID customerId;
    private final TransactionType type;
    private TransactionStatus status;
    private final Money amount;
    private final UUID sourceAccountId;
    private final UUID targetAccountId;
    private final Instant createdAt;
    private Instant updatedAt;

    public Transaction(
            TransactionId id,
            UUID customerId,
            TransactionType type,
            TransactionStatus status,
            Money amount,
            UUID sourceAccountId,
            UUID targetAccountId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Transaction receive(
            UUID customerId,
            TransactionType type,
            Money amount,
            UUID sourceAccountId,
            UUID targetAccountId) {
        if (type == TransactionType.DEPOSIT && targetAccountId == null) {
            throw new IllegalArgumentException("Deposit requires a target account");
        }
        if (type == TransactionType.WITHDRAWAL && sourceAccountId == null) {
            throw new IllegalArgumentException("Withdrawal requires a source account");
        }
        if (type == TransactionType.TRANSFER
                && (sourceAccountId == null || targetAccountId == null)) {
            throw new IllegalArgumentException(
                    "Transfer requires both a source and target account");
        }
        Instant now = Instant.now();
        return new Transaction(
                TransactionId.newId(),
                customerId,
                type,
                TransactionStatus.RECEIVED,
                amount,
                sourceAccountId,
                targetAccountId,
                now,
                now);
    }

    public void validate() {
        requireStatus(TransactionStatus.RECEIVED);
        status = TransactionStatus.VALIDATED;
        updatedAt = Instant.now();
    }

    public void markProcessing() {
        requireStatus(TransactionStatus.VALIDATED);
        status = TransactionStatus.PROCESSING;
        updatedAt = Instant.now();
    }

    public void complete() {
        requireStatus(TransactionStatus.PROCESSING);
        status = TransactionStatus.COMPLETED;
        updatedAt = Instant.now();
    }

    public void fail() {
        requireStatus(TransactionStatus.PROCESSING);
        status = TransactionStatus.FAILED;
        updatedAt = Instant.now();
    }

    private void requireStatus(TransactionStatus expected) {
        if (status != expected) {
            throw new ConflictException(
                    "Transaction " + id + " must be " + expected + " but is " + status);
        }
    }

    public TransactionId id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public TransactionType type() {
        return type;
    }

    public TransactionStatus status() {
        return status;
    }

    public Money amount() {
        return amount;
    }

    public UUID sourceAccountId() {
        return sourceAccountId;
    }

    public UUID targetAccountId() {
        return targetAccountId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
