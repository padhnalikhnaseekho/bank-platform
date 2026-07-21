package com.bankplatform.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.account.adapter.in.messaging.dto.TransactionCreatedEvent;
import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountId;
import com.bankplatform.account.domain.AccountType;
import com.bankplatform.account.domain.LedgerEntry;
import com.bankplatform.common.event.EventPublisher;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ApplyMoneyMovementUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ApplyMoneyMovementUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ApplyMoneyMovementUseCase(accountRepository, eventPublisher);
    }

    @Test
    void creditsTheTargetAccountOnDeposit() {
        Account account = Account.open(UUID.randomUUID(), "123456789012", AccountType.SAVINGS, "INR");
        String transactionId = UUID.randomUUID().toString();
        when(accountRepository.findById(account.id())).thenReturn(Optional.of(account));

        useCase.execute(new TransactionCreatedEvent(transactionId, "DEPOSIT", null, account.id().toString(),
                new BigDecimal("50.00"), "INR"));

        assertThat(account.balance().amount()).isEqualByComparingTo("50.00");
        verify(accountRepository).save(account);
        verify(accountRepository).saveLedgerEntry(any(LedgerEntry.class));
        verify(eventPublisher).publish(eq("money-deposited"), eq("Account"), eq(account.id().toString()), any());
    }

    @Test
    void publishesAFailedOutcomeWhenTheAccountIsNotFound() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(AccountId.of(accountId))).thenReturn(Optional.empty());

        useCase.execute(new TransactionCreatedEvent(UUID.randomUUID().toString(), "WITHDRAWAL",
                accountId.toString(), null, new BigDecimal("10.00"), "INR"));

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eq("money-withdrawn"), eq("Account"), eq(accountId.toString()),
                payloadCaptor.capture());
        assertThat(objectMapper.writeValueAsString(payloadCaptor.getValue())).contains("\"status\":\"FAILED\"");
    }

    @Test
    void publishesAFailedOutcomeWhenWithdrawalExceedsBalance() {
        Account account = Account.open(UUID.randomUUID(), "123456789012", AccountType.CURRENT, "INR");
        when(accountRepository.findById(account.id())).thenReturn(Optional.of(account));

        useCase.execute(new TransactionCreatedEvent(UUID.randomUUID().toString(), "WITHDRAWAL",
                account.id().toString(), null, new BigDecimal("500.00"), "INR"));

        verify(accountRepository, never()).save(any());
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eq("money-withdrawn"), eq("Account"), eq(account.id().toString()),
                payloadCaptor.capture());
        assertThat(objectMapper.writeValueAsString(payloadCaptor.getValue())).contains("\"status\":\"FAILED\"");
    }
}
