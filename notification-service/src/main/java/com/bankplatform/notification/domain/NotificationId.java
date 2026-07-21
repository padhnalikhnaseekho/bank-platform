package com.bankplatform.notification.domain;

import java.util.UUID;

public record NotificationId(UUID value) {

    public static NotificationId newId() {
        return new NotificationId(UUID.randomUUID());
    }

    public static NotificationId of(UUID value) {
        return new NotificationId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
