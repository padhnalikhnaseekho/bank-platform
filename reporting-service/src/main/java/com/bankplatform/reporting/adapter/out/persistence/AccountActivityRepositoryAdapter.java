package com.bankplatform.reporting.adapter.out.persistence;

import com.bankplatform.reporting.application.port.AccountActivityRepository;
import com.bankplatform.reporting.domain.AccountActivityEntry;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AccountActivityRepositoryAdapter implements AccountActivityRepository {

    private final AccountActivityJpaRepository jpaRepository;

    public AccountActivityRepositoryAdapter(AccountActivityJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(AccountActivityEntry entry) {
        jpaRepository.save(new AccountActivityEntity(entry.id(), entry.customerId(), entry.accountId(),
                entry.eventType(), entry.amount(), entry.currency(), entry.occurredAt()));
    }

    @Override
    public java.util.List<AccountActivityEntry> findByAccountAndPeriod(UUID accountId, Instant from, Instant to) {
        return jpaRepository.findByAccountIdAndOccurredAtBetweenOrderByOccurredAtAsc(accountId, from, to).stream()
                .map(e -> new AccountActivityEntry(e.getId(), e.getCustomerId(), e.getAccountId(), e.getEventType(),
                        e.getAmount(), e.getCurrency(), e.getOccurredAt()))
                .toList();
    }
}
