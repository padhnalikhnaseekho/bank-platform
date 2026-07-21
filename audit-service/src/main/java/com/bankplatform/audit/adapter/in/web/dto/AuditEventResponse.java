package com.bankplatform.audit.adapter.in.web.dto;

import com.bankplatform.audit.domain.AuditEvent;
import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String payload,
        String correlationId,
        Instant occurredAt,
        Instant storedAt) {

    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(event.id(), event.eventId(), event.eventType(), event.aggregateType(),
                event.aggregateId(), event.payload(), event.correlationId(), event.occurredAt(), event.storedAt());
    }
}
