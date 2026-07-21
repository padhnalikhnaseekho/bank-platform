package com.bankplatform.audit.domain;

import java.time.Instant;
import java.util.UUID;

/** Append-only — no update/delete methods, and the DB role loses UPDATE/DELETE on this table. */
public record AuditEvent(
        UUID id,
        UUID eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String payload,
        String correlationId,
        Instant occurredAt,
        Instant storedAt) {

    public static AuditEvent capture(UUID eventId, String eventType, String aggregateType, String aggregateId,
            String payload, String correlationId, Instant occurredAt) {
        return new AuditEvent(UUID.randomUUID(), eventId, eventType, aggregateType, aggregateId, payload,
                correlationId, occurredAt, Instant.now());
    }
}
