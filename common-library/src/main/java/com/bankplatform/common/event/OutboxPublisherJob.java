package com.bankplatform.common.event;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.ObjectMapper;

public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherJob.class);
    private static final int BATCH_SIZE = 50;
    private static final Duration DEFAULT_SEND_TIMEOUT = Duration.ofSeconds(10);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String producerName;
    private final Duration sendTimeout;

    public OutboxPublisherJob(
            OutboxRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            String producerName) {
        this(outboxRepository, kafkaTemplate, objectMapper, producerName, DEFAULT_SEND_TIMEOUT);
    }

    /**
     * This job runs on Spring's shared {@code @Scheduled} thread pool. Without a bounded wait here,
     * a hung Kafka send blocks that thread forever — in services with more than one scheduled task
     * (e.g. payment-service's due-payment poller shares the pool with this job), that silently
     * stops the other task too. See docs/adr/0011-bulkhead-isolation.md.
     */
    public OutboxPublisherJob(
            OutboxRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            String producerName,
            Duration sendTimeout) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.producerName = producerName;
        this.sendTimeout = sendTimeout;
    }

    @Scheduled(fixedDelayString = "${bank-platform.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        List<OutboxRecord> pending = outboxRepository.findPendingBatch(BATCH_SIZE);
        for (OutboxRecord record : pending) {
            publishOne(record);
        }
    }

    private void publishOne(OutboxRecord record) {
        try {
            EventEnvelope envelope =
                    new EventEnvelope(
                            record.id(),
                            record.eventType(),
                            record.eventVersion(),
                            record.createdAt(),
                            producerName,
                            record.correlationId(),
                            null,
                            record.aggregateType(),
                            record.aggregateId(),
                            record.aggregateId(),
                            objectMapper.readTree(record.payloadJson()));
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate
                    .send(record.eventType(), record.aggregateId(), json)
                    .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            outboxRepository.markPublished(record.id());
        } catch (Exception e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("Failed to publish outbox record {}: {}", record.id(), reason);
            outboxRepository.markFailed(record.id(), reason);
        }
    }
}
