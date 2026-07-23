package com.bankplatform.user.adapter.in.web;

import com.bankplatform.user.adapter.in.web.dto.AuthResponse;
import com.bankplatform.user.adapter.in.web.dto.LoginRequest;
import com.bankplatform.user.adapter.in.web.dto.RefreshRequest;
import com.bankplatform.user.adapter.in.web.dto.TokenResponse;
import com.bankplatform.user.adapter.in.web.dto.UserResponse;
import com.bankplatform.user.application.LoginUseCase;
import com.bankplatform.user.application.RefreshTokenUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    public AuthController(LoginUseCase loginUseCase, RefreshTokenUseCase refreshTokenUseCase) {
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        LoginUseCase.Result result = loginUseCase.login(request.email(), request.password());
        return new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                result.refreshTokenExpiresAt(),
                UserResponse.from(result.user()));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenUseCase.Result result = refreshTokenUseCase.refresh(request.refreshToken());
        return new TokenResponse(
                result.accessToken(), result.refreshToken(), result.refreshTokenExpiresAt());
    }
}
