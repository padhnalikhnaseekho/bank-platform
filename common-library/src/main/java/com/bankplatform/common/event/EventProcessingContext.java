package com.bankplatform.common.event;

import com.bankplatform.common.web.CorrelationIdFilter;
import org.slf4j.MDC;

/** The Kafka-consumer equivalent of {@link CorrelationIdFilter} for HTTP requests. */
public final class EventProcessingContext {

    private EventProcessingContext() {}

    public static void withCorrelation(EventEnvelope envelope, Runnable action) {
        String previous = MDC.get(CorrelationIdFilter.MDC_KEY);
        MDC.put(CorrelationIdFilter.MDC_KEY, envelope.correlationId());
        try {
            action.run();
        } finally {
            if (previous != null) {
                MDC.put(CorrelationIdFilter.MDC_KEY, previous);
            } else {
                MDC.remove(CorrelationIdFilter.MDC_KEY);
            }
        }
    }
}
