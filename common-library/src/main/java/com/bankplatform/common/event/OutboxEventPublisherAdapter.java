package com.bankplatform.common.event;

import com.bankplatform.common.web.CorrelationIdFilter;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes an outbox row inside the caller's existing transaction instead of publishing directly —
 * {@link OutboxPublisherJob} does the actual Kafka send afterward. Call sites (use cases) never
 * change between this and {@link NoOpEventPublisher}.
 */
public class OutboxEventPublisherAdapter implements EventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisherAdapter(
            OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(
            String eventType, String aggregateType, String aggregateId, Object payload) {
        String payloadJson = objectMapper.writeValueAsString(payload);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        outboxRepository.save(
                new OutboxRecord(
                        UUID.randomUUID(),
                        aggregateType,
                        aggregateId,
                        eventType,
                        1,
                        payloadJson,
                        correlationId,
                        Instant.now()));
    }
}
