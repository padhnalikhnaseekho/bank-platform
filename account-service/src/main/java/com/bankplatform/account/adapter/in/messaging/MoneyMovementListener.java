package com.bankplatform.account.adapter.in.messaging;

import com.bankplatform.account.adapter.in.messaging.dto.TransactionCreatedEvent;
import com.bankplatform.account.application.ApplyMoneyMovementUseCase;
import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.EventProcessingContext;
import com.bankplatform.common.event.IdempotentEventProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Handles deposit/withdrawal — ignores TRANSFER (that's TransferListener's job). */
@Component
public class MoneyMovementListener {

    private final ApplyMoneyMovementUseCase applyMoneyMovementUseCase;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final ObjectMapper objectMapper;

    public MoneyMovementListener(
            ApplyMoneyMovementUseCase applyMoneyMovementUseCase,
            IdempotentEventProcessor idempotentEventProcessor,
            ObjectMapper objectMapper) {
        this.applyMoneyMovementUseCase = applyMoneyMovementUseCase;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transaction-created")
    public void onTransactionCreated(String message) {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        EventProcessingContext.withCorrelation(
                envelope,
                () ->
                        idempotentEventProcessor.process(
                                envelope.eventId(),
                                envelope.eventType(),
                                () -> {
                                    TransactionCreatedEvent payload =
                                            objectMapper.convertValue(
                                                    envelope.payload(),
                                                    TransactionCreatedEvent.class);
                                    if (!"TRANSFER".equals(payload.type())) {
                                        applyMoneyMovementUseCase.execute(payload);
                                    }
                                }));
    }
}
