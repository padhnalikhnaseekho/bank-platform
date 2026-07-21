package com.bankplatform.payment.domain;

import java.util.UUID;

public record PaymentAttemptId(UUID value) {

    public static PaymentAttemptId newId() {
        return new PaymentAttemptId(UUID.randomUUID());
    }

    public static PaymentAttemptId of(UUID value) {
        return new PaymentAttemptId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
