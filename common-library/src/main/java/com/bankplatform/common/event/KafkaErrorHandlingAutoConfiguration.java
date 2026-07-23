package com.bankplatform.common.event;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Every service's auto-configured {@code ConcurrentKafkaListenerContainerFactory} picks up a single
 * {@link DefaultErrorHandler} bean automatically. Retries transient failures with exponential
 * backoff, then routes to the single shared {@code dead-letter} topic (plan/KAFKA.md:84-90) rather
 * than Spring Kafka's default per-topic {@code .DLT} suffix.
 */
@AutoConfiguration
public class KafkaErrorHandlingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DefaultErrorHandler.class)
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaOperations,
                        (record, ex) -> new TopicPartition("dead-letter", record.partition()));
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(30_000L);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
