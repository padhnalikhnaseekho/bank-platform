package com.bankplatform.reporting.adapter.in.messaging;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.EventProcessingContext;
import com.bankplatform.common.event.IdempotentEventProcessor;
import com.bankplatform.reporting.adapter.in.messaging.dto.AccountCreatedEvent;
import com.bankplatform.reporting.adapter.in.messaging.dto.MoneyMovementOutcomeEvent;
import com.bankplatform.reporting.adapter.in.messaging.dto.TransferOutcomeEvent;
import com.bankplatform.reporting.application.port.AccountActivityRepository;
import com.bankplatform.reporting.domain.AccountActivityEntry;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Builds the account_activity_view read model from the events Reporting Service consumes. */
@Component
public class AccountActivityEventListener {

    private final AccountActivityRepository accountActivityRepository;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final ObjectMapper objectMapper;

    public AccountActivityEventListener(AccountActivityRepository accountActivityRepository,
            IdempotentEventProcessor idempotentEventProcessor, ObjectMapper objectMapper) {
        this.accountActivityRepository = accountActivityRepository;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "account-created")
    public void onAccountCreated(String message) {
        withIdempotency(message, envelope -> {
            AccountCreatedEvent event = objectMapper.convertValue(envelope.payload(), AccountCreatedEvent.class);
            recordAccountCreated(event, envelope.occurredAt());
        });
    }

    @KafkaListener(topics = "money-deposited")
    public void onMoneyDeposited(String message) {
        withIdempotency(message, envelope -> {
            MoneyMovementOutcomeEvent event = objectMapper.convertValue(envelope.payload(),
                    MoneyMovementOutcomeEvent.class);
            recordMoneyMovement(event, "DEPOSIT", envelope.occurredAt());
        });
    }

    @KafkaListener(topics = "money-withdrawn")
    public void onMoneyWithdrawn(String message) {
        withIdempotency(message, envelope -> {
            MoneyMovementOutcomeEvent event = objectMapper.convertValue(envelope.payload(),
                    MoneyMovementOutcomeEvent.class);
            recordMoneyMovement(event, "WITHDRAWAL", envelope.occurredAt());
        });
    }

    @KafkaListener(topics = "transfer-completed")
    public void onTransferCompleted(String message) {
        withIdempotency(message, envelope -> {
            TransferOutcomeEvent event = objectMapper.convertValue(envelope.payload(), TransferOutcomeEvent.class);
            recordTransfer(event, envelope.occurredAt());
        });
    }

    @Transactional
    void recordAccountCreated(AccountCreatedEvent event, java.time.Instant occurredAt) {
        accountActivityRepository.save(AccountActivityEntry.create(UUID.fromString(event.customerId()),
                UUID.fromString(event.accountId()), "ACCOUNT_CREATED", event.balance(), event.currency(),
                occurredAt));
    }

    @Transactional
    void recordMoneyMovement(MoneyMovementOutcomeEvent event, String eventType, java.time.Instant occurredAt) {
        if (!"COMPLETED".equals(event.status()) || event.customerId() == null) {
            return;
        }
        accountActivityRepository.save(AccountActivityEntry.create(UUID.fromString(event.customerId()),
                UUID.fromString(event.accountId()), eventType, event.amount(), event.currency(), occurredAt));
    }

    @Transactional
    void recordTransfer(TransferOutcomeEvent event, java.time.Instant occurredAt) {
        if (event.sourceCustomerId() != null) {
            accountActivityRepository.save(AccountActivityEntry.create(UUID.fromString(event.sourceCustomerId()),
                    UUID.fromString(event.sourceAccountId()), "TRANSFER_OUT", event.amount(), event.currency(),
                    occurredAt));
        }
        if (event.targetCustomerId() != null) {
            accountActivityRepository.save(AccountActivityEntry.create(UUID.fromString(event.targetCustomerId()),
                    UUID.fromString(event.targetAccountId()), "TRANSFER_IN", event.amount(), event.currency(),
                    occurredAt));
        }
    }

    private void withIdempotency(String message, Consumer<EventEnvelope> handler) {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        EventProcessingContext.withCorrelation(envelope, () -> idempotentEventProcessor.process(envelope.eventId(),
                envelope.eventType(), () -> handler.accept(envelope)));
    }
}
