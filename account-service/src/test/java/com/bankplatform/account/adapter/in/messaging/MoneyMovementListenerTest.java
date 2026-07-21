package com.bankplatform.account.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountId;
import com.bankplatform.account.domain.AccountType;
import com.bankplatform.account.domain.LedgerEntry;
import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.common.event.IdempotentEventProcessor;
import java.math.BigDecimal;
import java.time.Instant;
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
class MoneyMovementListenerTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private IdempotentEventProcessor idempotentEventProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MoneyMovementListener listener;

    @BeforeEach
    void setUp() {
        listener = new MoneyMovementListener(accountRepository, eventPublisher, idempotentEventProcessor,
                objectMapper);
        doAnswer(invocation -> {
            Runnable handler = invocation.getArgument(2);
            handler.run();
            return null;
        }).when(idempotentEventProcessor).process(any(), any(), any());
    }

    private String toMessage(String eventType, Object payload, String aggregateId) {
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), eventType, 1, Instant.now(),
                "transaction-service", "corr-1", null, "Transaction", aggregateId, aggregateId, payload);
        return objectMapper.writeValueAsString(envelope);
    }

    private record TransactionCreatedPayload(String transactionId, String type, String sourceAccountId,
            String targetAccountId, BigDecimal amount, String currency) {}

    @Test
    void creditsTheTargetAccountOnDeposit() {
        Account account = Account.open(UUID.randomUUID(), "123456789012", AccountType.SAVINGS, "INR");
        String transactionId = UUID.randomUUID().toString();
        when(accountRepository.findById(account.id())).thenReturn(Optional.of(account));

        String message = toMessage("transaction-created",
                new TransactionCreatedPayload(transactionId, "DEPOSIT", null, account.id().toString(),
                        new BigDecimal("50.00"), "INR"),
                account.id().toString());

        listener.onTransactionCreated(message);

        assertThat(account.balance().amount()).isEqualByComparingTo("50.00");
        verify(accountRepository).save(account);
        verify(accountRepository).saveLedgerEntry(any(LedgerEntry.class));
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.eq("money-deposited"),
                org.mockito.ArgumentMatchers.eq("Account"), org.mockito.ArgumentMatchers.eq(account.id().toString()),
                any());
    }

    @Test
    void ignoresTransferEvents() {
        String message = toMessage("transaction-created",
                new TransactionCreatedPayload(UUID.randomUUID().toString(), "TRANSFER", UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), new BigDecimal("10.00"), "INR"),
                UUID.randomUUID().toString());

        listener.onTransactionCreated(message);

        verify(accountRepository, org.mockito.Mockito.never()).findById(any());
        verify(eventPublisher, org.mockito.Mockito.never()).publish(any(), any(), any(), any());
    }

    @Test
    void publishesAFailedOutcomeWhenTheAccountIsNotFound() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(AccountId.of(accountId))).thenReturn(Optional.empty());

        String message = toMessage("transaction-created", new TransactionCreatedPayload(
                UUID.randomUUID().toString(), "WITHDRAWAL", accountId.toString(), null, new BigDecimal("10.00"),
                "INR"), accountId.toString());

        listener.onTransactionCreated(message);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.eq("money-withdrawn"),
                org.mockito.ArgumentMatchers.eq("Account"), org.mockito.ArgumentMatchers.eq(accountId.toString()),
                payloadCaptor.capture());
        assertThat(objectMapper.writeValueAsString(payloadCaptor.getValue())).contains("\"status\":\"FAILED\"");
    }

    @Test
    void publishesAFailedOutcomeWhenWithdrawalExceedsBalance() {
        Account account = Account.open(UUID.randomUUID(), "123456789012", AccountType.CURRENT, "INR");
        when(accountRepository.findById(account.id())).thenReturn(Optional.of(account));

        String message = toMessage("transaction-created",
                new TransactionCreatedPayload(UUID.randomUUID().toString(), "WITHDRAWAL", account.id().toString(),
                        null, new BigDecimal("500.00"), "INR"),
                account.id().toString());

        listener.onTransactionCreated(message);

        verify(accountRepository, org.mockito.Mockito.never()).save(any());
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.eq("money-withdrawn"),
                org.mockito.ArgumentMatchers.eq("Account"), org.mockito.ArgumentMatchers.eq(account.id().toString()),
                payloadCaptor.capture());
        assertThat(objectMapper.writeValueAsString(payloadCaptor.getValue())).contains("\"status\":\"FAILED\"");
    }
}
