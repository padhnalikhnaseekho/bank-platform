package com.bankplatform.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpEventPublisher.class);

    @Override
    public void publish(
            String eventType, String aggregateType, String aggregateId, Object payload) {
        log.debug(
                "Event published (no-op): type={} aggregateType={} aggregateId={}",
                eventType,
                aggregateType,
                aggregateId);
    }
}
