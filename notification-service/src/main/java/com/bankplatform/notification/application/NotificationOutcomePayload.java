package com.bankplatform.notification.application;

public record NotificationOutcomePayload(String notificationId, String recipientUserId, String channel,
        String template) {}
