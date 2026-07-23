package com.bankplatform.user.application;

import com.bankplatform.common.error.ConflictException;
import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.user.application.port.PasswordHasher;
import com.bankplatform.user.application.port.UserRepository;
import com.bankplatform.user.domain.Email;
import com.bankplatform.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final EventPublisher eventPublisher;

    public RegisterUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public User register(String rawEmail, String phone, String fullName, String rawPassword) {
        Email email = new Email(rawEmail);
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        String passwordHash = passwordHasher.hash(rawPassword);
        User user = User.register(email, phone, fullName, passwordHash);
        User saved = userRepository.save(user);
        eventPublisher.publish(
                "user-created", "User", saved.id().toString(), UserCreatedPayload.from(saved));
        return saved;
    }
}
