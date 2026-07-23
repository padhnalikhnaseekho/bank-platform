package com.bankplatform.reporting.adapter.out.persistence;

import com.bankplatform.reporting.application.port.StatementJobRepository;
import com.bankplatform.reporting.domain.StatementId;
import com.bankplatform.reporting.domain.StatementJob;
import com.bankplatform.reporting.domain.StatementStatus;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class StatementJobRepositoryAdapter implements StatementJobRepository {

    private final StatementJobJpaRepository jpaRepository;

    public StatementJobRepositoryAdapter(StatementJobJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StatementJob save(StatementJob job) {
        StatementJobEntity saved =
                jpaRepository.save(
                        new StatementJobEntity(
                                job.id().value(),
                                job.customerId(),
                                job.accountId(),
                                job.periodStart(),
                                job.periodEnd(),
                                job.status().name(),
                                job.csvFileUrl(),
                                job.pdfFileUrl(),
                                job.createdAt(),
                                job.updatedAt()));
        return toDomain(saved);
    }

    @Override
    public Optional<StatementJob> findById(StatementId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    private StatementJob toDomain(StatementJobEntity entity) {
        return new StatementJob(
                StatementId.of(entity.getId()),
                entity.getCustomerId(),
                entity.getAccountId(),
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                StatementStatus.valueOf(entity.getStatus()),
                entity.getCsvFileUrl(),
                entity.getPdfFileUrl(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
