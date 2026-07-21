package com.bankplatform.account.adapter.in.messaging;

import com.bankplatform.account.adapter.in.messaging.dto.TransactionCreatedEvent;
import com.bankplatform.account.application.ApplyTransferUseCase;
import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.EventProcessingContext;
import com.bankplatform.common.event.IdempotentEventProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class TransferListener {

    private final ApplyTransferUseCase applyTransferUseCase;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final ObjectMapper objectMapper;

    public TransferListener(ApplyTransferUseCase applyTransferUseCase, IdempotentEventProcessor idempotentEventProcessor,
            ObjectMapper objectMapper) {
        this.applyTransferUseCase = applyTransferUseCase;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transfer-started")
    public void onTransferStarted(String message) {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        EventProcessingContext.withCorrelation(envelope,
                () -> idempotentEventProcessor.process(envelope.eventId(), envelope.eventType(), () -> {
                    TransactionCreatedEvent payload = objectMapper.convertValue(envelope.payload(),
                            TransactionCreatedEvent.class);
                    applyTransferUseCase.execute(payload);
                }));
    }
}
