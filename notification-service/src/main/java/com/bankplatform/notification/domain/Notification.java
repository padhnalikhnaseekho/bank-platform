package com.bankplatform.notification.domain;

import java.time.Instant;
import java.util.UUID;

public class Notification {

    private final NotificationId id;
    private final UUID recipientUserId;
    private final Channel channel;
    private final String template;
    private final String message;
    private NotificationStatus status;
    private final Instant createdAt;

    public Notification(NotificationId id, UUID recipientUserId, Channel channel, String template, String message,
            NotificationStatus status, Instant createdAt) {
        this.id = id;
        this.recipientUserId = recipientUserId;
        this.channel = channel;
        this.template = template;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Notification create(UUID recipientUserId, Channel channel, String template, String message) {
        return new Notification(NotificationId.newId(), recipientUserId, channel, template, message,
                NotificationStatus.PENDING, Instant.now());
    }

    public void markSent() {
        status = NotificationStatus.SENT;
    }

    public void markFailed() {
        status = NotificationStatus.FAILED;
    }

    public NotificationId id() {
        return id;
    }

    public UUID recipientUserId() {
        return recipientUserId;
    }

    public Channel channel() {
        return channel;
    }

    public String template() {
        return template;
    }

    public String message() {
        return message;
    }

    public NotificationStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
