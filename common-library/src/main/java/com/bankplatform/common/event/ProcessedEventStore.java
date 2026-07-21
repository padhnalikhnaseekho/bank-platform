package com.bankplatform.common.event;

import java.util.UUID;

/** Each consuming service implements this against its own {@code processed_events} table. */
public interface ProcessedEventStore {

    boolean isProcessed(UUID eventId);

    void markProcessed(UUID eventId, String eventType);
}
