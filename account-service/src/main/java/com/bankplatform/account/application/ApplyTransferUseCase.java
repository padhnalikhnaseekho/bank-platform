package com.bankplatform.account.application;

import com.bankplatform.account.adapter.in.messaging.dto.TransactionCreatedEvent;
import com.bankplatform.account.adapter.in.messaging.dto.TransferOutcomePayload;
import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountId;
import com.bankplatform.account.domain.LedgerEntry;
import com.bankplatform.account.domain.Money;
import com.bankplatform.common.error.ConflictException;
import com.bankplatform.common.error.ValidationException;
import com.bankplatform.common.event.EventPublisher;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kept as its own bean (rather than inlined in TransferListener) so the transactional boundary is
 * enforced through a real Spring proxy — calling an {@code @Transactional} method on {@code this}
 * from within the same class silently skips the transaction, since self-invocation never goes
 * through the proxy, in CGLIB just as much as JDK proxies.
 *
 * <p>Debits the source and credits the target in one transaction — both accounts live in this same
 * service's database, so no distributed saga/compensation is needed here.
 */
@Service
public class ApplyTransferUseCase {

    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;

    public ApplyTransferUseCase(
            AccountRepository accountRepository, EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(TransactionCreatedEvent payload) {
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
        eventPublisher.publish(
                eventType,
                "Transfer",
                payload.transactionId(),
                new TransferOutcomePayload(
                        payload.transactionId(),
                        payload.sourceAccountId(),
                        payload.targetAccountId(),
                        sourceCustomerId,
                        targetCustomerId,
                        payload.amount(),
                        payload.currency(),
                        failureReason));
    }
}
