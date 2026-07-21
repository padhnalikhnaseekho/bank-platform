package com.bankplatform.reporting.application;

import com.bankplatform.reporting.adapter.in.messaging.dto.AccountCreatedEvent;
import com.bankplatform.reporting.adapter.in.messaging.dto.MoneyMovementOutcomeEvent;
import com.bankplatform.reporting.adapter.in.messaging.dto.TransferOutcomeEvent;
import com.bankplatform.reporting.application.port.AccountActivityRepository;
import com.bankplatform.reporting.domain.AccountActivityEntry;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kept as its own bean (rather than inlined in AccountActivityEventListener) so the
 * transactional boundary is enforced through a real Spring proxy — calling an
 * {@code @Transactional} method on {@code this} from within the same class silently skips
 * the transaction, since self-invocation never goes through the proxy, in CGLIB just as
 * much as JDK proxies.
 */
@Service
public class RecordAccountActivityUseCase {

    private final AccountActivityRepository accountActivityRepository;

    public RecordAccountActivityUseCase(AccountActivityRepository accountActivityRepository) {
        this.accountActivityRepository = accountActivityRepository;
    }

    @Transactional
    public void recordAccountCreated(AccountCreatedEvent event, Instant occurredAt) {
        accountActivityRepository.save(AccountActivityEntry.create(UUID.fromString(event.customerId()),
                UUID.fromString(event.accountId()), "ACCOUNT_CREATED", event.balance(), event.currency(),
                occurredAt));
    }

    @Transactional
    public void recordMoneyMovement(MoneyMovementOutcomeEvent event, String eventType, Instant occurredAt) {
        if (!"COMPLETED".equals(event.status()) || event.customerId() == null) {
            return;
        }
        accountActivityRepository.save(AccountActivityEntry.create(UUID.fromString(event.customerId()),
                UUID.fromString(event.accountId()), eventType, event.amount(), event.currency(), occurredAt));
    }

    @Transactional
    public void recordTransfer(TransferOutcomeEvent event, Instant occurredAt) {
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
}
