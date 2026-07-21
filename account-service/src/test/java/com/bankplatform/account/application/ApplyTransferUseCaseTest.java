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
import com.bankplatform.account.domain.Money;
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
class ApplyTransferUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ApplyTransferUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ApplyTransferUseCase(accountRepository, eventPublisher);
    }

    @Test
    void debitsSourceAndCreditsTargetOnSuccessfulTransfer() {
        Account source = Account.open(UUID.randomUUID(), "111111111111", AccountType.SAVINGS, "INR");
        Account target = Account.open(UUID.randomUUID(), "222222222222", AccountType.SAVINGS, "INR");
        source.credit(Money.of(new BigDecimal("100.00"), "INR"), "seed");
        when(accountRepository.findById(source.id())).thenReturn(Optional.of(source));
        when(accountRepository.findById(target.id())).thenReturn(Optional.of(target));
        String transactionId = UUID.randomUUID().toString();

        useCase.execute(new TransactionCreatedEvent(transactionId, "TRANSFER", source.id().toString(),
                target.id().toString(), new BigDecimal("40.00"), "INR"));

        assertThat(source.balance().amount()).isEqualByComparingTo("60.00");
        assertThat(target.balance().amount()).isEqualByComparingTo("40.00");
        verify(accountRepository).save(source);
        verify(accountRepository).save(target);
        verify(eventPublisher).publish(eq("transfer-completed"), eq("Transfer"), eq(transactionId), any());
    }

    @Test
    void publishesTransferFailedWhenSourceAccountIsMissing() {
        UUID sourceId = UUID.randomUUID();
        Account target = Account.open(UUID.randomUUID(), "222222222222", AccountType.SAVINGS, "INR");
        when(accountRepository.findById(AccountId.of(sourceId))).thenReturn(Optional.empty());
        when(accountRepository.findById(target.id())).thenReturn(Optional.of(target));
        String transactionId = UUID.randomUUID().toString();

        useCase.execute(new TransactionCreatedEvent(transactionId, "TRANSFER", sourceId.toString(),
                target.id().toString(), new BigDecimal("40.00"), "INR"));

        verify(accountRepository, never()).save(any());
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eq("transfer-failed"), eq("Transfer"), eq(transactionId),
                payloadCaptor.capture());
        assertThat(objectMapper.writeValueAsString(payloadCaptor.getValue())).contains("Account not found");
    }

    @Test
    void publishesTransferFailedWhenSourceHasInsufficientFunds() {
        Account source = Account.open(UUID.randomUUID(), "111111111111", AccountType.SAVINGS, "INR");
        Account target = Account.open(UUID.randomUUID(), "222222222222", AccountType.SAVINGS, "INR");
        when(accountRepository.findById(source.id())).thenReturn(Optional.of(source));
        when(accountRepository.findById(target.id())).thenReturn(Optional.of(target));
        String transactionId = UUID.randomUUID().toString();

        useCase.execute(new TransactionCreatedEvent(transactionId, "TRANSFER", source.id().toString(),
                target.id().toString(), new BigDecimal("999.00"), "INR"));

        assertThat(source.balance().amount()).isEqualByComparingTo("0");
        assertThat(target.balance().amount()).isEqualByComparingTo("0");
        verify(accountRepository, never()).save(any());
        verify(eventPublisher).publish(eq("transfer-failed"), eq("Transfer"), eq(transactionId), any());
    }
}
