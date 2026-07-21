package com.bankplatform.transaction.adapter.in.messaging;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.EventProcessingContext;
import com.bankplatform.common.event.IdempotentEventProcessor;
import com.bankplatform.transaction.adapter.in.messaging.dto.MoneyMovementOutcomeEvent;
import com.bankplatform.transaction.adapter.in.messaging.dto.TransferOutcomeEvent;
import com.bankplatform.transaction.application.ApplyTransactionOutcomeUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class TransactionOutcomeListener {

    private final ApplyTransactionOutcomeUseCase applyTransactionOutcomeUseCase;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final ObjectMapper objectMapper;

    public TransactionOutcomeListener(ApplyTransactionOutcomeUseCase applyTransactionOutcomeUseCase,
            IdempotentEventProcessor idempotentEventProcessor, ObjectMapper objectMapper) {
        this.applyTransactionOutcomeUseCase = applyTransactionOutcomeUseCase;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "money-deposited")
    public void onMoneyDeposited(String message) {
        handleMoneyMovement(message);
    }

    @KafkaListener(topics = "money-withdrawn")
    public void onMoneyWithdrawn(String message) {
        handleMoneyMovement(message);
    }

    @KafkaListener(topics = "transfer-completed")
    public void onTransferCompleted(String message) {
        handleTransferOutcome(message, true);
    }

    @KafkaListener(topics = "transfer-failed")
    public void onTransferFailed(String message) {
        handleTransferOutcome(message, false);
    }

    private void handleMoneyMovement(String message) {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        EventProcessingContext.withCorrelation(envelope, () -> idempotentEventProcessor.process(envelope.eventId(),
                envelope.eventType(), () -> {
                    MoneyMovementOutcomeEvent payload = objectMapper.convertValue(envelope.payload(),
                            MoneyMovementOutcomeEvent.class);
                    applyTransactionOutcomeUseCase.execute(payload.transactionId(),
                            "COMPLETED".equals(payload.status()));
                }));
    }

    private void handleTransferOutcome(String message, boolean success) {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        EventProcessingContext.withCorrelation(envelope, () -> idempotentEventProcessor.process(envelope.eventId(),
                envelope.eventType(), () -> {
                    TransferOutcomeEvent payload = objectMapper.convertValue(envelope.payload(),
                            TransferOutcomeEvent.class);
                    applyTransactionOutcomeUseCase.execute(payload.transactionId(), success);
                }));
    }
}
