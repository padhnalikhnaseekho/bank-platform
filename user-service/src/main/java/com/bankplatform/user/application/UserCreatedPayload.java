package com.bankplatform.user.application;

import com.bankplatform.user.domain.User;
import java.util.List;

/**
 * Deliberately excludes {@code Credential} (password hash) — the raw {@link User} aggregate must
 * never be handed to {@code EventPublisher} directly.
 */
public record UserCreatedPayload(String userId, String email, String fullName, List<String> roles) {

    public static UserCreatedPayload from(User user) {
        return new UserCreatedPayload(
                user.id().toString(),
                user.email().value(),
                user.fullName(),
                user.roles().stream().map(Enum::name).toList());
    }
}
