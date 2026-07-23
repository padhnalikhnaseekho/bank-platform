package com.bankplatform.account.adapter.in.messaging;

import com.bankplatform.account.adapter.in.messaging.dto.FraudAlertEvent;
import com.bankplatform.account.application.FreezeAccountsForFraudAlertUseCase;
import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.EventProcessingContext;
import com.bankplatform.common.event.IdempotentEventProcessor;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class FraudAlertListener {

    private final FreezeAccountsForFraudAlertUseCase freezeAccountsForFraudAlertUseCase;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final ObjectMapper objectMapper;

    public FraudAlertListener(
            FreezeAccountsForFraudAlertUseCase freezeAccountsForFraudAlertUseCase,
            IdempotentEventProcessor idempotentEventProcessor,
            ObjectMapper objectMapper) {
        this.freezeAccountsForFraudAlertUseCase = freezeAccountsForFraudAlertUseCase;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "fraud-alert")
    public void onFraudAlert(String message) {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        EventProcessingContext.withCorrelation(
                envelope,
                () ->
                        idempotentEventProcessor.process(
                                envelope.eventId(),
                                envelope.eventType(),
                                () -> {
                                    FraudAlertEvent event =
                                            objectMapper.convertValue(
                                                    envelope.payload(), FraudAlertEvent.class);
                                    freezeAccountsForFraudAlertUseCase.execute(
                                            UUID.fromString(event.customerId()));
                                }));
    }
}
