package com.bankplatform.notification.adapter.out.persistence;

import com.bankplatform.common.event.ProcessedEventStore;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProcessedEventStoreAdapter implements ProcessedEventStore {

    private final ProcessedEventJpaRepository jpaRepository;

    public ProcessedEventStoreAdapter(ProcessedEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean isProcessed(UUID eventId) {
        return jpaRepository.existsById(eventId);
    }

    @Override
    public void markProcessed(UUID eventId, String eventType) {
        jpaRepository.save(new ProcessedEventEntity(eventId, eventType, Instant.now()));
    }
}
