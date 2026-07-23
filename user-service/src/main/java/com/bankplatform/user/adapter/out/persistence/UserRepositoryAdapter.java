package com.bankplatform.user.adapter.out.persistence;

import com.bankplatform.user.application.port.UserRepository;
import com.bankplatform.user.domain.Email;
import com.bankplatform.user.domain.Role;
import com.bankplatform.user.domain.User;
import com.bankplatform.user.domain.UserId;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;

    public UserRepositoryAdapter(
            UserJpaRepository userJpaRepository, RoleJpaRepository roleJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.roleJpaRepository = roleJpaRepository;
    }

    @Override
    public User save(User user) {
        UserEntity entity = UserMapper.toNewEntity(user, this::resolveRoleEntity);
        UserEntity saved = userJpaRepository.save(entity);
        return UserMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return userJpaRepository.findById(id.value()).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return userJpaRepository.findByEmail(email.value()).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return userJpaRepository.existsByEmail(email.value());
    }

    private RoleEntity resolveRoleEntity(Role role) {
        return roleJpaRepository
                .findByName(role.name())
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + role.name()));
    }
}
