package com.bankplatform.audit.adapter.out.persistence;

import com.bankplatform.audit.application.port.AuditEventRepository;
import com.bankplatform.audit.domain.AuditEvent;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class AuditEventRepositoryAdapter implements AuditEventRepository {

    private final AuditEventJpaRepository jpaRepository;

    public AuditEventRepositoryAdapter(AuditEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(AuditEvent event) {
        jpaRepository.save(new AuditEventEntity(event.id(), event.eventId(), event.eventType(),
                event.aggregateType(), event.aggregateId(), event.payload(), event.correlationId(),
                event.occurredAt(), event.storedAt()));
    }

    @Override
    public Page<AuditEvent> search(String aggregateId, String eventType, Instant from, Instant to,
            Pageable pageable) {
        return jpaRepository.search(aggregateId, eventType, from, to, pageable)
                .map(e -> new AuditEvent(e.getId(), e.getEventId(), e.getEventType(), e.getAggregateType(),
                        e.getAggregateId(), e.getPayload(), e.getHeaders(), e.getOccurredAt(), e.getStoredAt()));
    }
}
