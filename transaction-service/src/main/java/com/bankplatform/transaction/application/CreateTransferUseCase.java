package com.bankplatform.transaction.application;

import com.bankplatform.common.error.ValidationException;
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
public class CreateTransferUseCase {

    private final TransactionRepository transactionRepository;
    private final EventPublisher eventPublisher;

    public CreateTransferUseCase(
            TransactionRepository transactionRepository, EventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Transaction execute(
            UUID customerId, UUID sourceAccountId, UUID targetAccountId, Money amount) {
        if (sourceAccountId.equals(targetAccountId)) {
            throw new ValidationException("Source and target account must be different");
        }
        Transaction transaction =
                Transaction.receive(
                        customerId,
                        TransactionType.TRANSFER,
                        amount,
                        sourceAccountId,
                        targetAccountId);
        transaction.validate();
        transaction.markProcessing();
        Transaction saved = transactionRepository.save(transaction);

        eventPublisher.publish(
                "transfer-started",
                "Transaction",
                saved.id().toString(),
                new TransactionEventPayload(
                        saved.id().toString(),
                        "TRANSFER",
                        sourceAccountId.toString(),
                        targetAccountId.toString(),
                        amount.amount(),
                        amount.currency().getCurrencyCode()));
        return saved;
    }
}
