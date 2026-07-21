package com.bankplatform.user.application.port;

import com.bankplatform.user.domain.RefreshToken;
import com.bankplatform.user.domain.UserId;
import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revokeAllForUser(UserId userId);
}
