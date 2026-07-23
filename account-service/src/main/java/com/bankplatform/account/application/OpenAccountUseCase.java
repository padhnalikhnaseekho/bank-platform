package com.bankplatform.account.application;

import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountType;
import com.bankplatform.common.event.EventPublisher;
import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenAccountUseCase {

    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;
    private final SecureRandom random = new SecureRandom();

    public OpenAccountUseCase(AccountRepository accountRepository, EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Account open(UUID customerId, AccountType type, String currencyCode) {
        Account account =
                Account.open(customerId, generateUniqueAccountNumber(), type, currencyCode);
        Account saved = accountRepository.save(account);
        eventPublisher.publish(
                "account-created",
                "Account",
                saved.id().toString(),
                AccountEventPayload.from(saved));
        return saved;
    }

    private String generateUniqueAccountNumber() {
        String candidate;
        do {
            candidate = String.format("%012d", Math.abs(random.nextLong() % 1_000_000_000_000L));
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }
}
