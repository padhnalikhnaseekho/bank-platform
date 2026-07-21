package com.bankplatform.account.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountId;
import com.bankplatform.account.domain.AccountType;
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
class TransferListenerTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private IdempotentEventProcessor idempotentEventProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TransferListener listener;

    @BeforeEach
    void setUp() {
        listener = new TransferListener(accountRepository, eventPublisher, idempotentEventProcessor, objectMapper);
        doAnswer(invocation -> {
            Runnable handler = invocation.getArgument(2);
            handler.run();
            return null;
        }).when(idempotentEventProcessor).process(any(), any(), any());
    }

    private record TransferStartedPayload(String transactionId, String type, String sourceAccountId,
            String targetAccountId, BigDecimal amount, String currency) {}

    private String toMessage(Object payload, String aggregateId) {
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), "transfer-started", 1, Instant.now(),
                "transaction-service", "corr-1", null, "Transaction", aggregateId, aggregateId, payload);
        return objectMapper.writeValueAsString(envelope);
    }

    @Test
    void debitsSourceAndCreditsTargetOnSuccessfulTransfer() {
        Account source = Account.open(UUID.randomUUID(), "111111111111", AccountType.SAVINGS, "INR");
        Account target = Account.open(UUID.randomUUID(), "222222222222", AccountType.SAVINGS, "INR");
        source.credit(com.bankplatform.account.domain.Money.of(new BigDecimal("100.00"), "INR"), "seed");
        when(accountRepository.findById(source.id())).thenReturn(Optional.of(source));
        when(accountRepository.findById(target.id())).thenReturn(Optional.of(target));

        String transactionId = UUID.randomUUID().toString();
        String message = toMessage(new TransferStartedPayload(transactionId, "TRANSFER", source.id().toString(),
                target.id().toString(), new BigDecimal("40.00"), "INR"), transactionId);

        listener.onTransferStarted(message);

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
        String message = toMessage(new TransferStartedPayload(transactionId, "TRANSFER", sourceId.toString(),
                target.id().toString(), new BigDecimal("40.00"), "INR"), transactionId);

        listener.onTransferStarted(message);

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
        String message = toMessage(new TransferStartedPayload(transactionId, "TRANSFER", source.id().toString(),
                target.id().toString(), new BigDecimal("999.00"), "INR"), transactionId);

        listener.onTransferStarted(message);

        assertThat(source.balance().amount()).isEqualByComparingTo("0");
        assertThat(target.balance().amount()).isEqualByComparingTo("0");
        verify(accountRepository, never()).save(any());
        verify(eventPublisher).publish(eq("transfer-failed"), eq("Transfer"), eq(transactionId), any());
    }
}
