package com.bankplatform.user.application;

import com.bankplatform.user.domain.User;

public record UserLoginPayload(String userId, String email) {

    public static UserLoginPayload from(User user) {
        return new UserLoginPayload(user.id().toString(), user.email().value());
    }
}
