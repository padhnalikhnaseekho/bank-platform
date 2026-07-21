package com.bankplatform.user.adapter.in.web;

import com.bankplatform.user.adapter.in.web.dto.RegisterRequest;
import com.bankplatform.user.adapter.in.web.dto.UserResponse;
import com.bankplatform.user.application.GetCurrentUserUseCase;
import com.bankplatform.user.application.RegisterUserUseCase;
import com.bankplatform.user.domain.User;
import com.bankplatform.user.domain.UserId;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    public UserController(RegisterUserUseCase registerUserUseCase, GetCurrentUserUseCase getCurrentUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = registerUserUseCase.register(request.email(), request.phone(), request.fullName(),
                request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        UserId userId = UserId.of(UUID.fromString(jwt.getSubject()));
        User user = getCurrentUserUseCase.getById(userId);
        return UserResponse.from(user);
    }
}
