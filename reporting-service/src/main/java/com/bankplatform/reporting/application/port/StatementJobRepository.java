package com.bankplatform.reporting.application.port;

import com.bankplatform.reporting.domain.StatementId;
import com.bankplatform.reporting.domain.StatementJob;
import java.util.Optional;

public interface StatementJobRepository {

    StatementJob save(StatementJob job);

    Optional<StatementJob> findById(StatementId id);
}
