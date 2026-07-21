package com.bankplatform.notification.adapter.in.messaging.dto;

public record NotificationRequestedEvent(String recipientUserId, String channel, String template, String message) {}
