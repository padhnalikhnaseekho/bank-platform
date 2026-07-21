package com.bankplatform.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountId;
import com.bankplatform.account.domain.AccountStatus;
import com.bankplatform.account.domain.AccountType;
import com.bankplatform.common.error.NotFoundException;
import com.bankplatform.common.event.EventPublisher;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FreezeAccountUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EventPublisher eventPublisher;

    private FreezeAccountUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FreezeAccountUseCase(accountRepository, eventPublisher);
    }

    @Test
    void freezesExistingAccount() {
        Account account = Account.open(UUID.randomUUID(), "123456789012", AccountType.SAVINGS, "INR");
        AccountId id = account.id();
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account frozen = useCase.freeze(id);

        assertThat(frozen.status()).isEqualTo(AccountStatus.FROZEN);
    }

    @Test
    void rejectsUnknownAccount() {
        AccountId id = AccountId.newId();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.freeze(id)).isInstanceOf(NotFoundException.class);
    }
}
