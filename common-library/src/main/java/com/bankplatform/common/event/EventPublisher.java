package com.bankplatform.common.event;

/**
 * Publishes a domain event. Phase 1 wires only {@link NoOpEventPublisher}; Phase 2 replaces it with
 * an Outbox-backed Kafka adapter without touching call sites.
 */
public interface EventPublisher {

    void publish(String eventType, String aggregateType, String aggregateId, Object payload);
}
