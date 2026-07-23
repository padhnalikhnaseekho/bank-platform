package com.bankplatform.reporting.adapter.in.messaging;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.EventProcessingContext;
import com.bankplatform.common.event.IdempotentEventProcessor;
import com.bankplatform.reporting.adapter.in.messaging.dto.AccountCreatedEvent;
import com.bankplatform.reporting.adapter.in.messaging.dto.MoneyMovementOutcomeEvent;
import com.bankplatform.reporting.adapter.in.messaging.dto.TransferOutcomeEvent;
import com.bankplatform.reporting.application.RecordAccountActivityUseCase;
import java.util.function.Consumer;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Builds the account_activity_view read model from the events Reporting Service consumes. */
@Component
public class AccountActivityEventListener {

    private final RecordAccountActivityUseCase recordAccountActivityUseCase;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final ObjectMapper objectMapper;

    public AccountActivityEventListener(
            RecordAccountActivityUseCase recordAccountActivityUseCase,
            IdempotentEventProcessor idempotentEventProcessor,
            ObjectMapper objectMapper) {
        this.recordAccountActivityUseCase = recordAccountActivityUseCase;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "account-created")
    public void onAccountCreated(String message) {
        withIdempotency(
                message,
                envelope -> {
                    AccountCreatedEvent event =
                            objectMapper.convertValue(
                                    envelope.payload(), AccountCreatedEvent.class);
                    recordAccountActivityUseCase.recordAccountCreated(event, envelope.occurredAt());
                });
    }

    @KafkaListener(topics = "money-deposited")
    public void onMoneyDeposited(String message) {
        withIdempotency(
                message,
                envelope -> {
                    MoneyMovementOutcomeEvent event =
                            objectMapper.convertValue(
                                    envelope.payload(), MoneyMovementOutcomeEvent.class);
                    recordAccountActivityUseCase.recordMoneyMovement(
                            event, "DEPOSIT", envelope.occurredAt());
                });
    }

    @KafkaListener(topics = "money-withdrawn")
    public void onMoneyWithdrawn(String message) {
        withIdempotency(
                message,
                envelope -> {
                    MoneyMovementOutcomeEvent event =
                            objectMapper.convertValue(
                                    envelope.payload(), MoneyMovementOutcomeEvent.class);
                    recordAccountActivityUseCase.recordMoneyMovement(
                            event, "WITHDRAWAL", envelope.occurredAt());
                });
    }

    @KafkaListener(topics = "transfer-completed")
    public void onTransferCompleted(String message) {
        withIdempotency(
                message,
                envelope -> {
                    TransferOutcomeEvent event =
                            objectMapper.convertValue(
                                    envelope.payload(), TransferOutcomeEvent.class);
                    recordAccountActivityUseCase.recordTransfer(event, envelope.occurredAt());
                });
    }

    private void withIdempotency(String message, Consumer<EventEnvelope> handler) {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        EventProcessingContext.withCorrelation(
                envelope,
                () ->
                        idempotentEventProcessor.process(
                                envelope.eventId(),
                                envelope.eventType(),
                                () -> handler.accept(envelope)));
    }
}
