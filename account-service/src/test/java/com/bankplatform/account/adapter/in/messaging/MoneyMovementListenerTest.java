package com.bankplatform.account.adapter.in.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bankplatform.account.application.ApplyMoneyMovementUseCase;
import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.IdempotentEventProcessor;
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
class MoneyMovementListenerTest {

    @Mock
    private ApplyMoneyMovementUseCase applyMoneyMovementUseCase;

    @Mock
    private IdempotentEventProcessor idempotentEventProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MoneyMovementListener listener;

    @BeforeEach
    void setUp() {
        listener = new MoneyMovementListener(applyMoneyMovementUseCase, idempotentEventProcessor, objectMapper);
    }

    private void stubProcessorToRunHandler() {
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
    void delegatesDepositsAndWithdrawalsToTheUseCase() {
        stubProcessorToRunHandler();
        String transactionId = UUID.randomUUID().toString();
        UUID accountId = UUID.randomUUID();
        String message = toMessage("transaction-created",
                new TransactionCreatedPayload(transactionId, "DEPOSIT", null, accountId.toString(),
                        new BigDecimal("50.00"), "INR"),
                accountId.toString());

        listener.onTransactionCreated(message);

        verify(applyMoneyMovementUseCase).execute(any());
    }

    @Test
    void ignoresTransferEvents() {
        stubProcessorToRunHandler();
        String message = toMessage("transaction-created",
                new TransactionCreatedPayload(UUID.randomUUID().toString(), "TRANSFER", UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), new BigDecimal("10.00"), "INR"),
                UUID.randomUUID().toString());

        listener.onTransactionCreated(message);

        verify(applyMoneyMovementUseCase, never()).execute(any());
    }

    @Test
    void skipsAlreadyProcessedEvents() {
        String message = toMessage("transaction-created",
                new TransactionCreatedPayload(UUID.randomUUID().toString(), "DEPOSIT", null,
                        UUID.randomUUID().toString(), new BigDecimal("10.00"), "INR"),
                UUID.randomUUID().toString());

        listener.onTransactionCreated(message);

        verify(applyMoneyMovementUseCase, never()).execute(any());
    }
}
