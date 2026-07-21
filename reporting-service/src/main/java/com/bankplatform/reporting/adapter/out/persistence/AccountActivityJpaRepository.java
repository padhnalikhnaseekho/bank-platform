package com.bankplatform.reporting.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountActivityJpaRepository extends JpaRepository<AccountActivityEntity, UUID> {

    List<AccountActivityEntity> findByAccountIdAndOccurredAtBetweenOrderByOccurredAtAsc(UUID accountId, Instant from,
            Instant to);
}
