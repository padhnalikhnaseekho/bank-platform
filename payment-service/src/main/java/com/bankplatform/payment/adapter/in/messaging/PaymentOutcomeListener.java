package com.bankplatform.payment.adapter.in.messaging;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.EventProcessingContext;
import com.bankplatform.common.event.IdempotentEventProcessor;
import com.bankplatform.payment.adapter.in.messaging.dto.TransferOutcomeEvent;
import com.bankplatform.payment.application.ApplyPaymentOutcomeUseCase;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Closes the loop on a payment-triggered transfer. Every transfer outcome flows through
 * here (not just payment-originated ones); attempts with no matching transactionId are
 * ordinary customer transfers and are silently ignored.
 */
@Component
public class PaymentOutcomeListener {

    private final ApplyPaymentOutcomeUseCase applyPaymentOutcomeUseCase;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final ObjectMapper objectMapper;

    public PaymentOutcomeListener(ApplyPaymentOutcomeUseCase applyPaymentOutcomeUseCase,
            IdempotentEventProcessor idempotentEventProcessor, ObjectMapper objectMapper) {
        this.applyPaymentOutcomeUseCase = applyPaymentOutcomeUseCase;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transfer-completed")
    public void onTransferCompleted(String message) {
        handleOutcome(message, true);
    }

    @KafkaListener(topics = "transfer-failed")
    public void onTransferFailed(String message) {
        handleOutcome(message, false);
    }

    private void handleOutcome(String message, boolean success) {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        EventProcessingContext.withCorrelation(envelope,
                () -> idempotentEventProcessor.process(envelope.eventId(), envelope.eventType(), () -> {
                    TransferOutcomeEvent event = objectMapper.convertValue(envelope.payload(),
                            TransferOutcomeEvent.class);
                    applyPaymentOutcomeUseCase.execute(UUID.fromString(event.transactionId()), success,
                            event.failureReason());
                }));
    }
}
