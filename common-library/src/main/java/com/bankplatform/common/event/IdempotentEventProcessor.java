package com.bankplatform.common.event;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdempotentEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(IdempotentEventProcessor.class);

    private final ProcessedEventStore processedEventStore;

    public IdempotentEventProcessor(ProcessedEventStore processedEventStore) {
        this.processedEventStore = processedEventStore;
    }

    /** Skips {@code handler} (and does not re-record) if this event id was already processed. */
    public void process(UUID eventId, String eventType, Runnable handler) {
        if (processedEventStore.isProcessed(eventId)) {
            log.debug("Skipping already-processed event {} ({})", eventId, eventType);
            return;
        }
        handler.run();
        processedEventStore.markProcessed(eventId, eventType);
    }
}
