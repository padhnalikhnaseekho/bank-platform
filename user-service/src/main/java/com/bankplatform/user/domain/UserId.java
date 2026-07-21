package com.bankplatform.user.domain;

import java.util.UUID;

public record UserId(UUID value) {

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
