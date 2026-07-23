package com.bankplatform.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountStatus;
import com.bankplatform.account.domain.AccountType;
import com.bankplatform.common.event.EventPublisher;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class FreezeAccountsForFraudAlertUseCaseTest {

    @Mock private AccountRepository accountRepository;

    @Mock private EventPublisher eventPublisher;

    private FreezeAccountsForFraudAlertUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FreezeAccountsForFraudAlertUseCase(accountRepository, eventPublisher);
    }

    @Test
    void freezesEveryActiveAccountForTheCustomerAndPublishesAccountFrozen() {
        UUID customerId = UUID.randomUUID();
        Account first = Account.open(customerId, "111111111111", AccountType.SAVINGS, "INR");
        Account second = Account.open(customerId, "222222222222", AccountType.CURRENT, "INR");
        when(accountRepository.findByCustomerId(eq(customerId), eq(AccountStatus.ACTIVE), any()))
                .thenReturn(new PageImpl<>(List.of(first, second)));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(customerId);

        assertThat(first.status()).isEqualTo(AccountStatus.FROZEN);
        assertThat(second.status()).isEqualTo(AccountStatus.FROZEN);
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(eventPublisher)
                .publish(eq("account-frozen"), eq("Account"), eq(first.id().toString()), any());
        verify(eventPublisher)
                .publish(eq("account-frozen"), eq("Account"), eq(second.id().toString()), any());
    }

    @Test
    void doesNothingWhenTheCustomerHasNoActiveAccounts() {
        UUID customerId = UUID.randomUUID();
        when(accountRepository.findByCustomerId(eq(customerId), eq(AccountStatus.ACTIVE), any()))
                .thenReturn(new PageImpl<>(List.of()));

        useCase.execute(customerId);

        verify(accountRepository, org.mockito.Mockito.never()).save(any());
        verify(eventPublisher, org.mockito.Mockito.never()).publish(any(), any(), any(), any());
    }
}
