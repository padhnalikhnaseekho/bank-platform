package com.bankplatform.reporting.application;

import com.bankplatform.common.error.NotFoundException;
import com.bankplatform.reporting.application.port.StatementJobRepository;
import com.bankplatform.reporting.domain.StatementId;
import com.bankplatform.reporting.domain.StatementJob;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetStatementJobUseCase {

    private final StatementJobRepository statementJobRepository;

    public GetStatementJobUseCase(StatementJobRepository statementJobRepository) {
        this.statementJobRepository = statementJobRepository;
    }

    @Transactional(readOnly = true)
    public StatementJob getById(StatementId id, UUID requesterId, boolean isAdmin) {
        StatementJob job =
                statementJobRepository
                        .findById(id)
                        .orElseThrow(() -> new NotFoundException("Statement not found"));
        if (!isAdmin && !job.customerId().equals(requesterId)) {
            throw new AccessDeniedException("Not authorized to view this statement");
        }
        return job;
    }
}
