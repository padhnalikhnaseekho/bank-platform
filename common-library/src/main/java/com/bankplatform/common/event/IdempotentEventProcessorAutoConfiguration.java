package com.bankplatform.common.event;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnBean(ProcessedEventStore.class)
public class IdempotentEventProcessorAutoConfiguration {

    @Bean
    public IdempotentEventProcessor idempotentEventProcessor(ProcessedEventStore processedEventStore) {
        return new IdempotentEventProcessor(processedEventStore);
    }
}
