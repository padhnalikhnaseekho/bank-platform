package com.bankplatform.user.adapter.in.web.dto;

import com.bankplatform.user.domain.User;
import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String email, String phone, String fullName, String status,
        List<String> roles) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id().value(), user.email().value(), user.phone(), user.fullName(),
                user.status().name(), user.roles().stream().map(Enum::name).toList());
    }
}
