package com.bankplatform.common.event;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/** Runs after {@link OutboxAutoConfiguration} so the outbox-backed publisher wins when present. */
@AutoConfiguration
@AutoConfigureAfter(OutboxAutoConfiguration.class)
public class EventPublisherAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher eventPublisher() {
        return new NoOpEventPublisher();
    }
}
