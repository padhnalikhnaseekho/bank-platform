package com.bankplatform.transaction.adapter.in.messaging;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.EventProcessingContext;
import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.common.event.IdempotentEventProcessor;
import com.bankplatform.transaction.adapter.in.messaging.dto.MoneyMovementOutcomeEvent;
import com.bankplatform.transaction.adapter.in.messaging.dto.TransferOutcomeEvent;
import com.bankplatform.transaction.application.event.TransactionStatusChangedPayload;
import com.bankplatform.transaction.application.port.TransactionRepository;
import com.bankplatform.transaction.domain.Transaction;
import com.bankplatform.transaction.domain.TransactionId;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
public class TransactionOutcomeListener {

    private final TransactionRepository transactionRepository;
    private final EventPublisher eventPublisher;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final ObjectMapper objectMapper;

    public TransactionOutcomeListener(TransactionRepository transactionRepository, EventPublisher eventPublisher,
            IdempotentEventProcessor idempotentEventProcessor, ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
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
                    applyOutcome(payload.transactionId(), "COMPLETED".equals(payload.status()));
                }));
    }

    private void handleTransferOutcome(String message, boolean success) {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        EventProcessingContext.withCorrelation(envelope, () -> idempotentEventProcessor.process(envelope.eventId(),
                envelope.eventType(), () -> {
                    TransferOutcomeEvent payload = objectMapper.convertValue(envelope.payload(),
                            TransferOutcomeEvent.class);
                    applyOutcome(payload.transactionId(), success);
                }));
    }

    @Transactional
    void applyOutcome(String transactionIdValue, boolean success) {
        TransactionId id = TransactionId.of(UUID.fromString(transactionIdValue));
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unknown transaction: " + transactionIdValue));
        if (success) {
            transaction.complete();
        } else {
            transaction.fail();
        }
        Transaction saved = transactionRepository.save(transaction);
        eventPublisher.publish("transaction-status-changed", "Transaction", saved.id().toString(),
                new TransactionStatusChangedPayload(saved.id().toString(), saved.status().name()));
    }
}
