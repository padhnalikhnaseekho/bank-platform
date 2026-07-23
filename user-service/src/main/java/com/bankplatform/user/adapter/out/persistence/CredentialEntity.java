package com.bankplatform.user.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credentials")
public class CredentialEntity {

    @Id private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    protected CredentialEntity() {}

    public CredentialEntity(
            UUID id,
            UserEntity user,
            String passwordHash,
            Instant passwordChangedAt,
            int failedAttempts) {
        this.id = id;
        this.user = user;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = passwordChangedAt;
        this.failedAttempts = failedAttempts;
    }

    public UUID getId() {
        return id;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }
}
