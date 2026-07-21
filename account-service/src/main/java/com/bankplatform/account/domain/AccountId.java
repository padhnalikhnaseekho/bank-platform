package com.bankplatform.account.domain;

import java.util.UUID;

public record AccountId(UUID value) {

    public static AccountId newId() {
        return new AccountId(UUID.randomUUID());
    }

    public static AccountId of(UUID value) {
        return new AccountId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
