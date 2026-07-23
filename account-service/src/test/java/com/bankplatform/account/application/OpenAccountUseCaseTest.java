package com.bankplatform.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountType;
import com.bankplatform.common.event.EventPublisher;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenAccountUseCaseTest {

    @Mock private AccountRepository accountRepository;

    @Mock private EventPublisher eventPublisher;

    private OpenAccountUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new OpenAccountUseCase(accountRepository, eventPublisher);
    }

    @Test
    void opensAccountWithGeneratedAccountNumber() {
        when(accountRepository.existsByAccountNumber(any(String.class))).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID customerId = UUID.randomUUID();
        Account account = useCase.open(customerId, AccountType.SAVINGS, "INR");

        assertThat(account.customerId()).isEqualTo(customerId);
        assertThat(account.accountNumber()).hasSize(12);
        assertThat(account.balance().amount()).isEqualByComparingTo("0");
    }

    @Test
    void retriesOnAccountNumberCollision() {
        when(accountRepository.existsByAccountNumber(any(String.class)))
                .thenReturn(true, true, false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account account = useCase.open(UUID.randomUUID(), AccountType.CURRENT, "INR");

        assertThat(account).isNotNull();
    }
}
