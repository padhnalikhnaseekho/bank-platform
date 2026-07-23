package com.bankplatform.account.application;

import com.bankplatform.account.adapter.in.messaging.dto.MoneyMovementOutcomePayload;
import com.bankplatform.account.adapter.in.messaging.dto.TransactionCreatedEvent;
import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountId;
import com.bankplatform.account.domain.LedgerEntry;
import com.bankplatform.account.domain.Money;
import com.bankplatform.common.error.ConflictException;
import com.bankplatform.common.error.ValidationException;
import com.bankplatform.common.event.EventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kept as its own bean (rather than inlined in MoneyMovementListener) so the transactional boundary
 * is enforced through a real Spring proxy — calling an {@code @Transactional} method on {@code
 * this} from within the same class silently skips the transaction, since self-invocation never goes
 * through the proxy, in CGLIB just as much as JDK proxies.
 */
@Service
public class ApplyMoneyMovementUseCase {

    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;

    public ApplyMoneyMovementUseCase(
            AccountRepository accountRepository, EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(TransactionCreatedEvent payload) {
        boolean isDeposit = "DEPOSIT".equals(payload.type());
        UUID accountIdValue =
                UUID.fromString(isDeposit ? payload.targetAccountId() : payload.sourceAccountId());

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
                LedgerEntry entry =
                        isDeposit
                                ? acc.credit(amount, payload.transactionId())
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
        eventPublisher.publish(
                eventType,
                "Account",
                accountIdValue.toString(),
                new MoneyMovementOutcomePayload(
                        payload.transactionId(),
                        accountIdValue.toString(),
                        customerId,
                        payload.amount(),
                        payload.currency(),
                        status,
                        failureReason));
    }
}
