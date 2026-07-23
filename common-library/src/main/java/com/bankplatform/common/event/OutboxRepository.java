package com.bankplatform.common.event;

import java.util.List;
import java.util.UUID;

/**
 * Each service implements this against its own {@code outbox_events} table (no shared JPA entities
 * across services). {@link OutboxEventPublisherAdapter} writes through it inside the caller's
 * existing transaction; {@link OutboxPublisherJob} polls it.
 */
public interface OutboxRepository {

    void save(OutboxRecord record);

    List<OutboxRecord> findPendingBatch(int limit);

    void markPublished(UUID id);

    void markFailed(UUID id, String error);
}
