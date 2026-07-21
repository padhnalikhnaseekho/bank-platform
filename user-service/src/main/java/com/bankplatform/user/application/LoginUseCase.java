package com.bankplatform.user.application;

import com.bankplatform.common.error.UnauthorizedException;
import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.user.application.port.PasswordHasher;
import com.bankplatform.user.application.port.RefreshTokenRepository;
import com.bankplatform.user.application.port.TokenIssuer;
import com.bankplatform.user.application.port.UserRepository;
import com.bankplatform.user.domain.Email;
import com.bankplatform.user.domain.RefreshToken;
import com.bankplatform.user.domain.User;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUseCase {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OpaqueTokenGenerator opaqueTokenGenerator;
    private final EventPublisher eventPublisher;

    public LoginUseCase(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer,
            RefreshTokenRepository refreshTokenRepository, OpaqueTokenGenerator opaqueTokenGenerator,
            EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokenRepository = refreshTokenRepository;
        this.opaqueTokenGenerator = opaqueTokenGenerator;
        this.eventPublisher = eventPublisher;
    }

    public record Result(User user, String accessToken, String refreshToken, Instant refreshTokenExpiresAt) {}

    @Transactional
    public Result login(String rawEmail, String rawPassword) {
        Email email = new Email(rawEmail);
        User user = userRepository.findByEmail(email)
                .filter(u -> passwordHasher.matches(rawPassword, u.credential().passwordHash()))
                .orElseThrow(() -> {
                    eventPublisher.publish("user-login-failed", "User", null, new UserLoginFailedPayload(rawEmail));
                    return new UnauthorizedException("Invalid email or password");
                });
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is not active");
        }

        String accessToken = tokenIssuer.issueAccessToken(user);
        String rawRefreshToken = opaqueTokenGenerator.generate();
        Instant expiresAt = Instant.now().plus(REFRESH_TOKEN_TTL);
        refreshTokenRepository.save(
                RefreshToken.issue(user.id(), OpaqueTokenGenerator.hash(rawRefreshToken), expiresAt));

        eventPublisher.publish("user-login-succeeded", "User", user.id().toString(), UserLoginPayload.from(user));
        return new Result(user, accessToken, rawRefreshToken, expiresAt);
    }
}
