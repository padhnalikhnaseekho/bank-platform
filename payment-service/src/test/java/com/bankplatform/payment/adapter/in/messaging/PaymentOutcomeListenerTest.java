package com.bankplatform.payment.adapter.in.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.IdempotentEventProcessor;
import com.bankplatform.payment.application.ApplyPaymentOutcomeUseCase;
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
class PaymentOutcomeListenerTest {

    @Mock private ApplyPaymentOutcomeUseCase applyPaymentOutcomeUseCase;

    @Mock private IdempotentEventProcessor idempotentEventProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PaymentOutcomeListener listener;

    private record TransferOutcomePayload(
            String transactionId,
            String sourceAccountId,
            String targetAccountId,
            String sourceCustomerId,
            String targetCustomerId,
            BigDecimal amount,
            String currency,
            String failureReason) {}

    @BeforeEach
    void setUp() {
        listener =
                new PaymentOutcomeListener(
                        applyPaymentOutcomeUseCase, idempotentEventProcessor, objectMapper);
    }

    private void stubProcessorToRunHandler() {
        doAnswer(
                        invocation -> {
                            Runnable handler = invocation.getArgument(2);
                            handler.run();
                            return null;
                        })
                .when(idempotentEventProcessor)
                .process(any(), any(), any());
    }

    private String toMessage(String eventType, UUID transactionId, String failureReason) {
        TransferOutcomePayload payload =
                new TransferOutcomePayload(
                        transactionId.toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        new BigDecimal("50.00"),
                        "INR",
                        failureReason);
        EventEnvelope envelope =
                new EventEnvelope(
                        UUID.randomUUID(),
                        eventType,
                        1,
                        Instant.now(),
                        "account-service",
                        "corr-1",
                        null,
                        "Transfer",
                        transactionId.toString(),
                        transactionId.toString(),
                        payload);
        return objectMapper.writeValueAsString(envelope);
    }

    @Test
    void delegatesTransferCompletedAsSuccess() {
        stubProcessorToRunHandler();
        UUID transactionId = UUID.randomUUID();

        listener.onTransferCompleted(toMessage("transfer-completed", transactionId, null));

        verify(applyPaymentOutcomeUseCase).execute(eq(transactionId), eq(true), any());
    }

    @Test
    void delegatesTransferFailedAsFailure() {
        stubProcessorToRunHandler();
        UUID transactionId = UUID.randomUUID();

        listener.onTransferFailed(
                toMessage("transfer-failed", transactionId, "Insufficient funds"));

        verify(applyPaymentOutcomeUseCase)
                .execute(eq(transactionId), eq(false), eq("Insufficient funds"));
    }

    @Test
    void skipsAlreadyProcessedEvents() {
        UUID transactionId = UUID.randomUUID();

        listener.onTransferCompleted(toMessage("transfer-completed", transactionId, null));

        verify(applyPaymentOutcomeUseCase, never()).execute(any(), anyBoolean(), any());
    }
}
