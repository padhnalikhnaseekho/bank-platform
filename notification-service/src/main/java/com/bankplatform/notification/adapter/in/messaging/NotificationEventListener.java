package com.bankplatform.notification.adapter.in.messaging;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.EventProcessingContext;
import com.bankplatform.common.event.IdempotentEventProcessor;
import com.bankplatform.notification.adapter.in.messaging.dto.FraudAlertEvent;
import com.bankplatform.notification.adapter.in.messaging.dto.MoneyMovementOutcomeEvent;
import com.bankplatform.notification.adapter.in.messaging.dto.NotificationRequestedEvent;
import com.bankplatform.notification.adapter.in.messaging.dto.TransferOutcomeEvent;
import com.bankplatform.notification.adapter.in.messaging.dto.UserCreatedEvent;
import com.bankplatform.notification.application.SendNotificationUseCase;
import com.bankplatform.notification.domain.Channel;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final SendNotificationUseCase sendNotificationUseCase;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final ObjectMapper objectMapper;

    public NotificationEventListener(
            SendNotificationUseCase sendNotificationUseCase,
            IdempotentEventProcessor idempotentEventProcessor,
            ObjectMapper objectMapper) {
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "user-created")
    public void onUserCreated(String message) {
        withIdempotency(
                message,
                envelope -> {
                    UserCreatedEvent event =
                            objectMapper.convertValue(envelope.payload(), UserCreatedEvent.class);
                    sendNotificationUseCase.send(
                            UUID.fromString(event.userId()),
                            Channel.EMAIL,
                            "welcome",
                            "Welcome to Bank Platform, " + event.fullName() + "!");
                });
    }

    @KafkaListener(topics = "money-deposited")
    public void onMoneyDeposited(String message) {
        handleMoneyMovement(message, "deposit-confirmation");
    }

    @KafkaListener(topics = "money-withdrawn")
    public void onMoneyWithdrawn(String message) {
        handleMoneyMovement(message, "withdrawal-confirmation");
    }

    @KafkaListener(topics = "transfer-completed")
    public void onTransferCompleted(String message) {
        handleTransfer(message, "transfer-confirmation");
    }

    @KafkaListener(topics = "transfer-failed")
    public void onTransferFailed(String message) {
        handleTransfer(message, "transfer-failure");
    }

    @KafkaListener(topics = "fraud-alert")
    public void onFraudAlert(String message) {
        withIdempotency(
                message,
                envelope -> {
                    FraudAlertEvent event =
                            objectMapper.convertValue(envelope.payload(), FraudAlertEvent.class);
                    sendNotificationUseCase.send(
                            UUID.fromString(event.customerId()),
                            Channel.EMAIL,
                            "fraud-alert",
                            event.message());
                });
    }

    @KafkaListener(topics = "notification-requested")
    public void onNotificationRequested(String message) {
        withIdempotency(
                message,
                envelope -> {
                    NotificationRequestedEvent event =
                            objectMapper.convertValue(
                                    envelope.payload(), NotificationRequestedEvent.class);
                    sendNotificationUseCase.send(
                            UUID.fromString(event.recipientUserId()),
                            Channel.valueOf(event.channel()),
                            event.template(),
                            event.message());
                });
    }

    private void handleMoneyMovement(String message, String template) {
        withIdempotency(
                message,
                envelope -> {
                    MoneyMovementOutcomeEvent event =
                            objectMapper.convertValue(
                                    envelope.payload(), MoneyMovementOutcomeEvent.class);
                    if (event.customerId() == null) {
                        log.warn(
                                "No customerId on {} for transaction {}; skipping notification",
                                envelope.eventType(),
                                event.transactionId());
                        return;
                    }
                    String text =
                            String.format(
                                    "%s of %s %s — %s",
                                    template, event.amount(), event.currency(), event.status());
                    sendNotificationUseCase.send(
                            UUID.fromString(event.customerId()), Channel.EMAIL, template, text);
                });
    }

    private void handleTransfer(String message, String template) {
        withIdempotency(
                message,
                envelope -> {
                    TransferOutcomeEvent event =
                            objectMapper.convertValue(
                                    envelope.payload(), TransferOutcomeEvent.class);
                    String text =
                            String.format(
                                    "%s of %s %s", template, event.amount(), event.currency());
                    if (event.sourceCustomerId() != null) {
                        sendNotificationUseCase.send(
                                UUID.fromString(event.sourceCustomerId()),
                                Channel.EMAIL,
                                template,
                                text);
                    }
                    if (event.targetCustomerId() != null) {
                        sendNotificationUseCase.send(
                                UUID.fromString(event.targetCustomerId()),
                                Channel.EMAIL,
                                template,
                                text);
                    }
                });
    }

    private void withIdempotency(
            String message, java.util.function.Consumer<EventEnvelope> handler) {
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
