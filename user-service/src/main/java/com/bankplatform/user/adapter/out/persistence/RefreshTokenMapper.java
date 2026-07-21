package com.bankplatform.user.adapter.out.persistence;

import com.bankplatform.user.domain.RefreshToken;
import com.bankplatform.user.domain.UserId;

final class RefreshTokenMapper {

    private RefreshTokenMapper() {}

    static RefreshToken toDomain(RefreshTokenEntity entity) {
        return new RefreshToken(entity.getId(), UserId.of(entity.getUserId()), entity.getTokenHash(),
                entity.getExpiresAt(), entity.getRevokedAt());
    }

    static RefreshTokenEntity toEntity(RefreshToken refreshToken) {
        return new RefreshTokenEntity(refreshToken.id(), refreshToken.userId().value(), refreshToken.tokenHash(),
                refreshToken.expiresAt(), refreshToken.revokedAt());
    }
}
