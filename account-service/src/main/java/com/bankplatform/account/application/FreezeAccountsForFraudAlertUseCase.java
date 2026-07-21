package com.bankplatform.account.application;

import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountStatus;
import com.bankplatform.common.event.EventPublisher;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Protective response to a fraud alert: freezes every ACTIVE account belonging to the
 * flagged customer pending review. A page size of 100 covers any realistic number of
 * accounts per customer; a customer with more than that would need a follow-up page, which
 * isn't handled here.
 */
@Service
public class FreezeAccountsForFraudAlertUseCase {

    private static final int PAGE_SIZE = 100;

    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;

    public FreezeAccountsForFraudAlertUseCase(AccountRepository accountRepository, EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(UUID customerId) {
        Page<Account> activeAccounts = accountRepository.findByCustomerId(customerId, AccountStatus.ACTIVE,
                PageRequest.of(0, PAGE_SIZE));
        for (Account account : activeAccounts) {
            account.freeze();
            Account saved = accountRepository.save(account);
            eventPublisher.publish("account-frozen", "Account", saved.id().toString(),
                    AccountEventPayload.from(saved));
        }
    }
}
