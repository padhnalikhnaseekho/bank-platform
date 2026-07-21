package com.bankplatform.transaction.adapter.in.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.IdempotentEventProcessor;
import com.bankplatform.transaction.application.ApplyTransactionOutcomeUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class TransactionOutcomeListenerTest {

    @Mock
    private ApplyTransactionOutcomeUseCase applyTransactionOutcomeUseCase;

    @Mock
    private IdempotentEventProcessor idempotentEventProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TransactionOutcomeListener listener;

    private record MoneyMovementPayload(String transactionId, String accountId, BigDecimal amount, String currency,
            String status, String failureReason) {}

    private record TransferPayload(String transactionId, String sourceAccountId, String targetAccountId,
            BigDecimal amount, String currency, String failureReason) {}

    @BeforeEach
    void setUp() {
        listener = new TransactionOutcomeListener(applyTransactionOutcomeUseCase, idempotentEventProcessor,
                objectMapper);
    }

    private void stubProcessorToRunHandler() {
        doAnswer(invocation -> {
            Runnable handler = invocation.getArgument(2);
            handler.run();
            return null;
        }).when(idempotentEventProcessor).process(any(), any(), any());
    }

    private String toMessage(String eventType, Object payload, String transactionId) {
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), eventType, 1, Instant.now(), "account-service",
                "corr-1", null, "Transaction", transactionId, transactionId, payload);
        return objectMapper.writeValueAsString(envelope);
    }

    @Test
    void moneyDepositedCompletedDelegatesAsSuccess() {
        stubProcessorToRunHandler();
        String transactionId = UUID.randomUUID().toString();
        String message = toMessage("money-deposited", new MoneyMovementPayload(transactionId,
                UUID.randomUUID().toString(), new BigDecimal("50.00"), "INR", "COMPLETED", null), transactionId);

        listener.onMoneyDeposited(message);

        verify(applyTransactionOutcomeUseCase).execute(transactionId, true);
    }

    @Test
    void moneyWithdrawnFailedDelegatesAsFailure() {
        stubProcessorToRunHandler();
        String transactionId = UUID.randomUUID().toString();
        String message = toMessage("money-withdrawn",
                new MoneyMovementPayload(transactionId, UUID.randomUUID().toString(), new BigDecimal("50.00"), "INR",
                        "FAILED", "Insufficient funds"),
                transactionId);

        listener.onMoneyWithdrawn(message);

        verify(applyTransactionOutcomeUseCase).execute(transactionId, false);
    }

    @Test
    void transferCompletedDelegatesAsSuccess() {
        stubProcessorToRunHandler();
        String transactionId = UUID.randomUUID().toString();
        String message = toMessage("transfer-completed",
                new TransferPayload(transactionId, UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                        new BigDecimal("40.00"), "INR", null),
                transactionId);

        listener.onTransferCompleted(message);

        verify(applyTransactionOutcomeUseCase).execute(transactionId, true);
    }

    @Test
    void transferFailedDelegatesAsFailure() {
        stubProcessorToRunHandler();
        String transactionId = UUID.randomUUID().toString();
        String message = toMessage("transfer-failed",
                new TransferPayload(transactionId, UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                        new BigDecimal("40.00"), "INR", "Account not found"),
                transactionId);

        listener.onTransferFailed(message);

        verify(applyTransactionOutcomeUseCase).execute(transactionId, false);
    }

    @Test
    void skipsAlreadyProcessedEvents() {
        String transactionId = UUID.randomUUID().toString();
        String message = toMessage("money-deposited", new MoneyMovementPayload(transactionId,
                UUID.randomUUID().toString(), new BigDecimal("50.00"), "INR", "COMPLETED", null), transactionId);

        listener.onMoneyDeposited(message);

        verify(applyTransactionOutcomeUseCase, never()).execute(any(), anyBoolean());
    }
}
