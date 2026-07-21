package com.bankplatform.payment.adapter.out.persistence;

import com.bankplatform.common.event.OutboxRecord;
import com.bankplatform.common.event.OutboxRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class OutboxRepositoryAdapter implements OutboxRepository {

    private final OutboxEventJpaRepository jpaRepository;

    public OutboxRepositoryAdapter(OutboxEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(OutboxRecord record) {
        jpaRepository.save(new OutboxEventEntity(record.id(), record.aggregateType(), record.aggregateId(),
                record.eventType(), record.eventVersion(), record.payloadJson(), record.correlationId(),
                record.createdAt()));
    }

    @Override
    public List<OutboxRecord> findPendingBatch(int limit) {
        return jpaRepository.findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, limit)).stream()
                .map(e -> new OutboxRecord(e.getId(), e.getAggregateType(), e.getAggregateId(), e.getEventType(),
                        e.getEventVersion(), e.getPayload(), e.getCorrelationId(), e.getCreatedAt()))
                .toList();
    }

    @Override
    public void markPublished(UUID id) {
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.markPublished(Instant.now());
            jpaRepository.save(entity);
        });
    }

    @Override
    public void markFailed(UUID id, String error) {
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.markFailed(error);
            jpaRepository.save(entity);
        });
    }
}
