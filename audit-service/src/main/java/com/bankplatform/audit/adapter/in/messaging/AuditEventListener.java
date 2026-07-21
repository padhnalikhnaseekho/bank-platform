package com.bankplatform.audit.adapter.in.messaging;

import com.bankplatform.audit.application.RecordAuditEventUseCase;
import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.EventProcessingContext;
import com.bankplatform.common.event.IdempotentEventProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Subscribes to every topic with a real producer in this phase. */
@Component
public class AuditEventListener {

    private final RecordAuditEventUseCase recordAuditEventUseCase;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final ObjectMapper objectMapper;

    public AuditEventListener(RecordAuditEventUseCase recordAuditEventUseCase,
            IdempotentEventProcessor idempotentEventProcessor, ObjectMapper objectMapper) {
        this.recordAuditEventUseCase = recordAuditEventUseCase;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = { "user-created", "user-login-succeeded", "user-login-failed", "account-created",
            "account-frozen", "money-deposited", "money-withdrawn", "transfer-started", "transfer-completed",
            "transfer-failed", "transaction-created", "transaction-status-changed", "notification-requested",
            "notification-sent", "notification-failed", "fraud-alert" })
    public void onEvent(String message) {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        EventProcessingContext.withCorrelation(envelope, () -> idempotentEventProcessor.process(envelope.eventId(),
                envelope.eventType(), () -> {
                    String payloadJson = objectMapper.writeValueAsString(envelope.payload());
                    recordAuditEventUseCase.record(envelope, payloadJson);
                }));
    }
}
