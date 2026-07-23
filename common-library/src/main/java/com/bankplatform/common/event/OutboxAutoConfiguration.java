package com.bankplatform.common.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.ObjectMapper;

/**
 * Activates only for services that define their own {@link OutboxRepository} bean (against their
 * own outbox table). Runs before {@link EventPublisherAutoConfiguration} so the outbox-backed
 * publisher wins over the no-op default when both are eligible.
 */
@AutoConfiguration
@EnableScheduling
@ConditionalOnBean(OutboxRepository.class)
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher outboxEventPublisher(
            OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        return new OutboxEventPublisherAdapter(outboxRepository, objectMapper);
    }

    @Bean
    public OutboxPublisherJob outboxPublisherJob(
            OutboxRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${spring.application.name}") String producerName) {
        return new OutboxPublisherJob(outboxRepository, kafkaTemplate, objectMapper, producerName);
    }
}
