package com.bankplatform.user.domain;

import java.time.Instant;
import java.util.Set;

public class User {

    public enum Status {
        ACTIVE,
        DISABLED
    }

    private final UserId id;
    private final Email email;
    private final String phone;
    private final String fullName;
    private final Status status;
    private final Set<Role> roles;
    private final Credential credential;
    private final Instant createdAt;
    private final Instant updatedAt;

    public User(
            UserId id,
            Email email,
            String phone,
            String fullName,
            Status status,
            Set<Role> roles,
            Credential credential,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.phone = phone;
        this.fullName = fullName;
        this.status = status;
        this.roles = Set.copyOf(roles);
        this.credential = credential;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User register(Email email, String phone, String fullName, String passwordHash) {
        UserId id = UserId.newId();
        Instant now = Instant.now();
        Credential credential = Credential.create(id, passwordHash);
        return new User(
                id,
                email,
                phone,
                fullName,
                Status.ACTIVE,
                Set.of(Role.CUSTOMER),
                credential,
                now,
                now);
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public String phone() {
        return phone;
    }

    public String fullName() {
        return fullName;
    }

    public Status status() {
        return status;
    }

    public Set<Role> roles() {
        return roles;
    }

    public Credential credential() {
        return credential;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
