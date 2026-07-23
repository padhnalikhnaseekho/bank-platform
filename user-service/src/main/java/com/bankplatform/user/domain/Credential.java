package com.bankplatform.user.domain;

import java.time.Instant;

public class Credential {

    private final UserId userId;
    private final String passwordHash;
    private final Instant passwordChangedAt;
    private final int failedAttempts;

    public Credential(
            UserId userId, String passwordHash, Instant passwordChangedAt, int failedAttempts) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = passwordChangedAt;
        this.failedAttempts = failedAttempts;
    }

    public static Credential create(UserId userId, String passwordHash) {
        return new Credential(userId, passwordHash, Instant.now(), 0);
    }

    public UserId userId() {
        return userId;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Instant passwordChangedAt() {
        return passwordChangedAt;
    }

    public int failedAttempts() {
        return failedAttempts;
    }
}
