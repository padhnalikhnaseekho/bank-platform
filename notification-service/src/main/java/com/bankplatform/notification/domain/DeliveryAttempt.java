package com.bankplatform.notification.domain;

import java.time.Instant;
import java.util.UUID;

public class DeliveryAttempt {

    private final UUID id;
    private final NotificationId notificationId;
    private final boolean success;
    private final String failureReason;
    private final Instant attemptedAt;

    public DeliveryAttempt(
            UUID id,
            NotificationId notificationId,
            boolean success,
            String failureReason,
            Instant attemptedAt) {
        this.id = id;
        this.notificationId = notificationId;
        this.success = success;
        this.failureReason = failureReason;
        this.attemptedAt = attemptedAt;
    }

    public static DeliveryAttempt of(
            NotificationId notificationId, boolean success, String failureReason) {
        return new DeliveryAttempt(
                UUID.randomUUID(), notificationId, success, failureReason, Instant.now());
    }

    public UUID id() {
        return id;
    }

    public NotificationId notificationId() {
        return notificationId;
    }

    public boolean success() {
        return success;
    }

    public String failureReason() {
        return failureReason;
    }

    public Instant attemptedAt() {
        return attemptedAt;
    }
}
