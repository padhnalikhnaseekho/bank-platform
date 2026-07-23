package com.bankplatform.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.common.error.ConflictException;
import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.user.application.port.PasswordHasher;
import com.bankplatform.user.application.port.UserRepository;
import com.bankplatform.user.domain.Email;
import com.bankplatform.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock private UserRepository userRepository;

    @Mock private PasswordHasher passwordHasher;

    @Mock private EventPublisher eventPublisher;

    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCase(userRepository, passwordHasher, eventPublisher);
    }

    @Test
    void registersNewUser() {
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordHasher.hash(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User user = useCase.register("alice@example.com", "+911234567890", "Alice", "password123");

        assertThat(user.email().value()).isEqualTo("alice@example.com");
        assertThat(user.credential().passwordHash()).isEqualTo("hashed");
        verify(eventPublisher)
                .publish(
                        "user-created",
                        "User",
                        user.id().toString(),
                        UserCreatedPayload.from(user));
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        assertThatThrownBy(
                        () -> useCase.register("alice@example.com", null, "Alice", "password123"))
                .isInstanceOf(ConflictException.class);
    }
}
