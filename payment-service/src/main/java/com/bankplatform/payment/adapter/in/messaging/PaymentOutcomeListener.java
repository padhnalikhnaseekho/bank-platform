package com.bankplatform.payment.adapter.in.messaging;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.EventProcessingContext;
import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.common.event.IdempotentEventProcessor;
import com.bankplatform.payment.adapter.in.messaging.dto.TransferOutcomeEvent;
import com.bankplatform.payment.application.event.PaymentOutcomePayload;
import com.bankplatform.payment.application.port.PaymentAttemptRepository;
import com.bankplatform.payment.domain.PaymentAttempt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Closes the loop on a payment-triggered transfer. Every transfer outcome flows through
 * here (not just payment-originated ones); attempts with no matching transactionId are
 * ordinary customer transfers and are silently ignored.
 */
@Component
public class PaymentOutcomeListener {

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final EventPublisher eventPublisher;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final ObjectMapper objectMapper;

    public PaymentOutcomeListener(PaymentAttemptRepository paymentAttemptRepository, EventPublisher eventPublisher,
            IdempotentEventProcessor idempotentEventProcessor, ObjectMapper objectMapper) {
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.eventPublisher = eventPublisher;
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
                    applyOutcome(UUID.fromString(event.transactionId()), success, event.failureReason());
                }));
    }

    @Transactional
    void applyOutcome(UUID transactionId, boolean success, String failureReason) {
        Optional<PaymentAttempt> maybeAttempt = paymentAttemptRepository.findByTransactionId(transactionId);
        if (maybeAttempt.isEmpty()) {
            return;
        }
        PaymentAttempt attempt = maybeAttempt.get();
        if (success) {
            attempt.markSucceeded();
        } else {
            attempt.markFailed(failureReason);
        }
        paymentAttemptRepository.save(attempt);

        String eventType = success ? "payment-success" : "payment-failed";
        eventPublisher.publish(eventType, "PaymentAttempt", attempt.id().toString(),
                new PaymentOutcomePayload(attempt.paymentInstructionId().toString(), attempt.id().toString(),
                        transactionId.toString(), attempt.status().name(), attempt.failureReason()));
    }
}
