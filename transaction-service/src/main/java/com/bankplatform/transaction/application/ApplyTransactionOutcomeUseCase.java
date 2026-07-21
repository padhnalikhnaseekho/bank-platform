package com.bankplatform.transaction.application;

import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.transaction.application.event.TransactionStatusChangedPayload;
import com.bankplatform.transaction.application.port.TransactionRepository;
import com.bankplatform.transaction.domain.Transaction;
import com.bankplatform.transaction.domain.TransactionId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kept as its own bean (rather than inlined in TransactionOutcomeListener) so the
 * transactional boundary is enforced through a real Spring proxy — calling an
 * {@code @Transactional} method on {@code this} from within the same class silently skips
 * the transaction, since self-invocation never goes through the proxy, in CGLIB just as
 * much as JDK proxies.
 */
@Service
public class ApplyTransactionOutcomeUseCase {

    private final TransactionRepository transactionRepository;
    private final EventPublisher eventPublisher;

    public ApplyTransactionOutcomeUseCase(TransactionRepository transactionRepository,
            EventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(String transactionIdValue, boolean success) {
        TransactionId id = TransactionId.of(UUID.fromString(transactionIdValue));
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unknown transaction: " + transactionIdValue));
        if (success) {
            transaction.complete();
        } else {
            transaction.fail();
        }
        Transaction saved = transactionRepository.save(transaction);
        eventPublisher.publish("transaction-status-changed", "Transaction", saved.id().toString(),
                new TransactionStatusChangedPayload(saved.id().toString(), saved.status().name()));
    }
}
