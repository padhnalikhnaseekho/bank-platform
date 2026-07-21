package com.bankplatform.transaction.domain;

import java.util.UUID;

public record TransactionId(UUID value) {

    public static TransactionId newId() {
        return new TransactionId(UUID.randomUUID());
    }

    public static TransactionId of(UUID value) {
        return new TransactionId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
