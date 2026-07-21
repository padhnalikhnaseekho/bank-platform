package com.bankplatform.common.event;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.ObjectMapper;

public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherJob.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String producerName;

    public OutboxPublisherJob(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper, String producerName) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.producerName = producerName;
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
            EventEnvelope envelope = new EventEnvelope(record.id(), record.eventType(), record.eventVersion(),
                    record.createdAt(), producerName, record.correlationId(), null, record.aggregateType(),
                    record.aggregateId(), record.aggregateId(), objectMapper.readTree(record.payloadJson()));
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(record.eventType(), record.aggregateId(), json).get();
            outboxRepository.markPublished(record.id());
        } catch (Exception e) {
            log.warn("Failed to publish outbox record {}: {}", record.id(), e.getMessage());
            outboxRepository.markFailed(record.id(), e.getMessage());
        }
    }
}
