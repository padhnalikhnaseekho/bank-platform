package com.bankplatform.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bankplatform.common.error.UnauthorizedException;
import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.user.application.port.PasswordHasher;
import com.bankplatform.user.application.port.RefreshTokenRepository;
import com.bankplatform.user.application.port.TokenIssuer;
import com.bankplatform.user.application.port.UserRepository;
import com.bankplatform.user.domain.Credential;
import com.bankplatform.user.domain.Email;
import com.bankplatform.user.domain.RefreshToken;
import com.bankplatform.user.domain.Role;
import com.bankplatform.user.domain.User;
import com.bankplatform.user.domain.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EventPublisher eventPublisher;

    private LoginUseCase useCase;

    private User activeUser;

    @BeforeEach
    void setUp() {
        useCase = new LoginUseCase(userRepository, passwordHasher, tokenIssuer, refreshTokenRepository,
                new OpaqueTokenGenerator(), eventPublisher);
        UserId id = UserId.newId();
        Credential credential = new Credential(id, "hashed", Instant.now(), 0);
        activeUser = new User(id, new Email("alice@example.com"), null, "Alice", User.Status.ACTIVE,
                Set.of(Role.CUSTOMER), credential, Instant.now(), Instant.now());
    }

    @Test
    void issuesTokensOnValidCredentials() {
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));
        when(passwordHasher.matches(anyString(), anyString())).thenReturn(true);
        when(tokenIssuer.issueAccessToken(activeUser)).thenReturn("jwt-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        LoginUseCase.Result result = useCase.login("alice@example.com", "password123");

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.refreshToken()).isNotBlank();
    }

    @Test
    void rejectsWrongPassword() {
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));
        when(passwordHasher.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> useCase.login("alice@example.com", "wrong"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsUnknownEmail() {
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.login("unknown@example.com", "password123"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
