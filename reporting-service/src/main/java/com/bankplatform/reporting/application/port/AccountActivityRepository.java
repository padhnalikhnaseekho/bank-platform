package com.bankplatform.reporting.application.port;

import com.bankplatform.reporting.domain.AccountActivityEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AccountActivityRepository {

    void save(AccountActivityEntry entry);

    List<AccountActivityEntry> findByAccountAndPeriod(UUID accountId, Instant from, Instant to);
}
