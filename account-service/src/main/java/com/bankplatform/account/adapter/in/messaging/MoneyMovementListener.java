package com.bankplatform.account.adapter.in.messaging;

import com.bankplatform.account.adapter.in.messaging.dto.MoneyMovementOutcomePayload;
import com.bankplatform.account.adapter.in.messaging.dto.TransactionCreatedEvent;
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
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Handles deposit/withdrawal — ignores TRANSFER (that's TransferListener's job). */
@Component
public class MoneyMovementListener {

    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final ObjectMapper objectMapper;

    public MoneyMovementListener(AccountRepository accountRepository, EventPublisher eventPublisher,
            IdempotentEventProcessor idempotentEventProcessor, ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transaction-created")
    public void onTransactionCreated(String message) {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        EventProcessingContext.withCorrelation(envelope,
                () -> idempotentEventProcessor.process(envelope.eventId(), envelope.eventType(), () -> {
                    TransactionCreatedEvent payload = objectMapper.convertValue(envelope.payload(),
                            TransactionCreatedEvent.class);
                    if (!"TRANSFER".equals(payload.type())) {
                        applyMoneyMovement(payload);
                    }
                }));
    }

    @Transactional
    void applyMoneyMovement(TransactionCreatedEvent payload) {
        boolean isDeposit = "DEPOSIT".equals(payload.type());
        UUID accountIdValue = UUID.fromString(isDeposit ? payload.targetAccountId() : payload.sourceAccountId());

        String status;
        String failureReason = null;
        String customerId = null;
        var account = accountRepository.findById(AccountId.of(accountIdValue));
        if (account.isEmpty()) {
            status = "FAILED";
            failureReason = "Account not found";
        } else {
            try {
                Account acc = account.get();
                customerId = acc.customerId().toString();
                Money amount = Money.of(payload.amount(), payload.currency());
                LedgerEntry entry = isDeposit ? acc.credit(amount, payload.transactionId())
                        : acc.debit(amount, payload.transactionId());
                accountRepository.save(acc);
                accountRepository.saveLedgerEntry(entry);
                status = "COMPLETED";
            } catch (ConflictException | ValidationException e) {
                status = "FAILED";
                failureReason = e.getMessage();
            }
        }

        String eventType = isDeposit ? "money-deposited" : "money-withdrawn";
        eventPublisher.publish(eventType, "Account", accountIdValue.toString(),
                new MoneyMovementOutcomePayload(payload.transactionId(), accountIdValue.toString(), customerId,
                        payload.amount(), payload.currency(), status, failureReason));
    }
}
