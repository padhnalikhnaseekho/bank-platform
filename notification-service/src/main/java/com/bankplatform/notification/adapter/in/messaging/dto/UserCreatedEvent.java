package com.bankplatform.notification.adapter.in.messaging.dto;

import java.util.List;

public record UserCreatedEvent(String userId, String email, String fullName, List<String> roles) {}
