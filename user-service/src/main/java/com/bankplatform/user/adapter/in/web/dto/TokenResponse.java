package com.bankplatform.user.adapter.in.web.dto;

import java.time.Instant;

public record TokenResponse(String accessToken, String refreshToken, Instant refreshTokenExpiresAt) {}
