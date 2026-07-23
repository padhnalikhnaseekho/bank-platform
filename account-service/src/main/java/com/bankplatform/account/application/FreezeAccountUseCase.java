package com.bankplatform.account.application;

import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountId;
import com.bankplatform.common.error.NotFoundException;
import com.bankplatform.common.event.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FreezeAccountUseCase {

    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;

    public FreezeAccountUseCase(
            AccountRepository accountRepository, EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Account freeze(AccountId id) {
        Account account =
                accountRepository
                        .findById(id)
                        .orElseThrow(() -> new NotFoundException("Account not found"));
        account.freeze();
        Account saved = accountRepository.save(account);
        eventPublisher.publish(
                "account-frozen",
                "Account",
                saved.id().toString(),
                AccountEventPayload.from(saved));
        return saved;
    }
}
