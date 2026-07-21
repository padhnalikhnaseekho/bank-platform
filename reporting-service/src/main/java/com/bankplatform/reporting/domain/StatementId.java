package com.bankplatform.reporting.domain;

import java.util.UUID;

public record StatementId(UUID value) {

    public static StatementId newId() {
        return new StatementId(UUID.randomUUID());
    }

    public static StatementId of(UUID value) {
        return new StatementId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
