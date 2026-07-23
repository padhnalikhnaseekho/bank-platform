package com.bankplatform.user.adapter.out.persistence;

import com.bankplatform.user.application.port.RefreshTokenRepository;
import com.bankplatform.user.domain.RefreshToken;
import com.bankplatform.user.domain.UserId;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository refreshTokenJpaRepository) {
        this.refreshTokenJpaRepository = refreshTokenJpaRepository;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity saved =
                refreshTokenJpaRepository.save(RefreshTokenMapper.toEntity(refreshToken));
        return RefreshTokenMapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return refreshTokenJpaRepository
                .findByTokenHash(tokenHash)
                .map(RefreshTokenMapper::toDomain);
    }

    @Override
    public void revokeAllForUser(UserId userId) {
        refreshTokenJpaRepository.revokeAllForUser(userId.value(), Instant.now());
    }
}
