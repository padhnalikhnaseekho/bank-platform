package com.bankplatform.user.adapter.out.persistence;

import com.bankplatform.user.domain.Credential;
import com.bankplatform.user.domain.Email;
import com.bankplatform.user.domain.Role;
import com.bankplatform.user.domain.User;
import com.bankplatform.user.domain.UserId;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

final class UserMapper {

    private UserMapper() {}

    static User toDomain(UserEntity entity) {
        Set<Role> roles =
                entity.getRoles().stream()
                        .map(r -> Role.valueOf(r.getName()))
                        .collect(Collectors.toSet());
        Credential credential =
                new Credential(
                        UserId.of(entity.getId()),
                        entity.getCredential().getPasswordHash(),
                        entity.getCredential().getPasswordChangedAt(),
                        entity.getCredential().getFailedAttempts());
        return new User(
                UserId.of(entity.getId()),
                new Email(entity.getEmail()),
                entity.getPhone(),
                entity.getFullName(),
                User.Status.valueOf(entity.getStatus()),
                roles,
                credential,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    static UserEntity toNewEntity(User user, Function<Role, RoleEntity> roleResolver) {
        Set<RoleEntity> roleEntities =
                user.roles().stream().map(roleResolver).collect(Collectors.toSet());
        UserEntity entity =
                new UserEntity(
                        user.id().value(),
                        user.email().value(),
                        user.phone(),
                        user.fullName(),
                        user.status().name(),
                        roleEntities,
                        user.createdAt(),
                        user.updatedAt());
        Credential credential = user.credential();
        entity.setCredential(
                new CredentialEntity(
                        UUID.randomUUID(),
                        entity,
                        credential.passwordHash(),
                        credential.passwordChangedAt(),
                        credential.failedAttempts()));
        return entity;
    }
}
