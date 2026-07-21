package com.bankplatform.transaction.adapter.out.persistence;

import com.bankplatform.transaction.application.port.IdempotencyRepository;
import com.bankplatform.transaction.domain.IdempotencyRecord;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyRepositoryAdapter implements IdempotencyRepository {

    private final IdempotencyRecordJpaRepository jpaRepository;

    public IdempotencyRepositoryAdapter(IdempotencyRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<IdempotencyRecord> findByKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(e -> new IdempotencyRecord(e.getId(),
                e.getIdempotencyKey(), e.getRequestHash(), e.getResponseBody(), e.getStatusCode(), e.getCreatedAt(),
                e.getExpiresAt()));
    }

    @Override
    public void save(IdempotencyRecord record) {
        jpaRepository.save(new IdempotencyRecordEntity(record.id(), record.idempotencyKey(), record.requestHash(),
                record.responseBody(), record.statusCode(), record.createdAt(), record.expiresAt()));
    }
}
