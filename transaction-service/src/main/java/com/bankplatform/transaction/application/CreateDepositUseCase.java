package com.bankplatform.transaction.application;

import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.transaction.application.event.TransactionEventPayload;
import com.bankplatform.transaction.application.port.TransactionRepository;
import com.bankplatform.transaction.domain.Money;
import com.bankplatform.transaction.domain.Transaction;
import com.bankplatform.transaction.domain.TransactionType;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateDepositUseCase {

    private final TransactionRepository transactionRepository;
    private final EventPublisher eventPublisher;

    public CreateDepositUseCase(TransactionRepository transactionRepository, EventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Transaction execute(UUID customerId, UUID accountId, Money amount) {
        Transaction transaction = Transaction.receive(customerId, TransactionType.DEPOSIT, amount, null, accountId);
        transaction.validate();
        transaction.markProcessing();
        Transaction saved = transactionRepository.save(transaction);

        eventPublisher.publish("transaction-created", "Transaction", saved.id().toString(),
                new TransactionEventPayload(saved.id().toString(), "DEPOSIT", null, accountId.toString(),
                        amount.amount(), amount.currency().getCurrencyCode()));
        return saved;
    }
}
