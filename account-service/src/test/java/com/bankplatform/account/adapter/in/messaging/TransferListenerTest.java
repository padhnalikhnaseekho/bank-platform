package com.bankplatform.account.adapter.in.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bankplatform.account.application.ApplyTransferUseCase;
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
class TransferListenerTest {

    @Mock
    private ApplyTransferUseCase applyTransferUseCase;

    @Mock
    private IdempotentEventProcessor idempotentEventProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TransferListener listener;

    @BeforeEach
    void setUp() {
        listener = new TransferListener(applyTransferUseCase, idempotentEventProcessor, objectMapper);
    }

    private void stubProcessorToRunHandler() {
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
    void delegatesToTheUseCase() {
        stubProcessorToRunHandler();
        String transactionId = UUID.randomUUID().toString();
        String message = toMessage(new TransferStartedPayload(transactionId, "TRANSFER", UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), new BigDecimal("40.00"), "INR"), transactionId);

        listener.onTransferStarted(message);

        verify(applyTransferUseCase).execute(any());
    }

    @Test
    void skipsAlreadyProcessedEvents() {
        String transactionId = UUID.randomUUID().toString();
        String message = toMessage(new TransferStartedPayload(transactionId, "TRANSFER", UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), new BigDecimal("40.00"), "INR"), transactionId);

        listener.onTransferStarted(message);

        verify(applyTransferUseCase, never()).execute(any());
    }
}
