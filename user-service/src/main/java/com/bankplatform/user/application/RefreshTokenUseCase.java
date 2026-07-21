package com.bankplatform.user.application;

import com.bankplatform.common.error.UnauthorizedException;
import com.bankplatform.user.application.port.RefreshTokenRepository;
import com.bankplatform.user.application.port.TokenIssuer;
import com.bankplatform.user.application.port.UserRepository;
import com.bankplatform.user.domain.RefreshToken;
import com.bankplatform.user.domain.User;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenUseCase {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenIssuer tokenIssuer;
    private final OpaqueTokenGenerator opaqueTokenGenerator;

    public RefreshTokenUseCase(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository,
            TokenIssuer tokenIssuer, OpaqueTokenGenerator opaqueTokenGenerator) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.tokenIssuer = tokenIssuer;
        this.opaqueTokenGenerator = opaqueTokenGenerator;
    }

    public record Result(String accessToken, String refreshToken, Instant refreshTokenExpiresAt) {}

    @Transactional
    public Result refresh(String rawRefreshToken) {
        String tokenHash = OpaqueTokenGenerator.hash(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(t -> t.isValid(Instant.now()))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        existing.revoke();
        refreshTokenRepository.save(existing);

        User user = userRepository.findById(existing.userId())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));

        String accessToken = tokenIssuer.issueAccessToken(user);
        String rawNewRefreshToken = opaqueTokenGenerator.generate();
        Instant expiresAt = Instant.now().plus(REFRESH_TOKEN_TTL);
        refreshTokenRepository.save(
                RefreshToken.issue(user.id(), OpaqueTokenGenerator.hash(rawNewRefreshToken), expiresAt));

        return new Result(accessToken, rawNewRefreshToken, expiresAt);
    }
}
