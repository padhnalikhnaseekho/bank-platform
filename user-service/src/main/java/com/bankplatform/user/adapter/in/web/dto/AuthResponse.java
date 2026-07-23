package com.bankplatform.user.adapter.in.web.dto;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UserResponse user) {}
