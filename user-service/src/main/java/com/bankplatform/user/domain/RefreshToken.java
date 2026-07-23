package com.bankplatform.user.domain;

import java.time.Instant;
import java.util.UUID;

public class RefreshToken {

    private final UUID id;
    private final UserId userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant revokedAt;

    public RefreshToken(
            UUID id, UserId userId, String tokenHash, Instant expiresAt, Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public static RefreshToken issue(UserId userId, String tokenHash, Instant expiresAt) {
        return new RefreshToken(UUID.randomUUID(), userId, tokenHash, expiresAt, null);
    }

    public boolean isValid(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }
}
