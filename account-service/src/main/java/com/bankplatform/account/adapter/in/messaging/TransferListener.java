package com.bankplatform.account.adapter.in.messaging;

import com.bankplatform.account.adapter.in.messaging.dto.TransactionCreatedEvent;
import com.bankplatform.account.adapter.in.messaging.dto.TransferOutcomePayload;
import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountId;
import com.bankplatform.account.domain.LedgerEntry;
import com.bankplatform.account.domain.Money;
import com.bankplatform.common.error.ConflictException;
import com.bankplatform.common.error.ValidationException;
import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.EventProcessingContext;
import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.common.event.IdempotentEventProcessor;
import java.util.Optional;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Debits the source and credits the target in one transaction — both accounts live in
 * this same service's database, so no distributed saga/compensation is needed here.
 */
@Component
public class TransferListener {

    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final ObjectMapper objectMapper;

    public TransferListener(AccountRepository accountRepository, EventPublisher eventPublisher,
            IdempotentEventProcessor idempotentEventProcessor, ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
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
                    applyTransfer(payload);
                }));
    }

    @Transactional
    void applyTransfer(TransactionCreatedEvent payload) {
        UUID sourceId = UUID.fromString(payload.sourceAccountId());
        UUID targetId = UUID.fromString(payload.targetAccountId());
        Optional<Account> source = accountRepository.findById(AccountId.of(sourceId));
        Optional<Account> target = accountRepository.findById(AccountId.of(targetId));

        boolean success;
        String failureReason = null;
        String sourceCustomerId = source.map(a -> a.customerId().toString()).orElse(null);
        String targetCustomerId = target.map(a -> a.customerId().toString()).orElse(null);
        if (source.isEmpty() || target.isEmpty()) {
            success = false;
            failureReason = "Account not found";
        } else {
            try {
                Account sourceAccount = source.get();
                Account targetAccount = target.get();
                Money amount = Money.of(payload.amount(), payload.currency());
                LedgerEntry debitEntry = sourceAccount.debit(amount, payload.transactionId());
                LedgerEntry creditEntry = targetAccount.credit(amount, payload.transactionId());
                accountRepository.save(sourceAccount);
                accountRepository.save(targetAccount);
                accountRepository.saveLedgerEntry(debitEntry);
                accountRepository.saveLedgerEntry(creditEntry);
                success = true;
            } catch (ConflictException | ValidationException e) {
                success = false;
                failureReason = e.getMessage();
            }
        }

        String eventType = success ? "transfer-completed" : "transfer-failed";
        eventPublisher.publish(eventType, "Transfer", payload.transactionId(),
                new TransferOutcomePayload(payload.transactionId(), payload.sourceAccountId(),
                        payload.targetAccountId(), sourceCustomerId, targetCustomerId, payload.amount(),
                        payload.currency(), failureReason));
    }
}
