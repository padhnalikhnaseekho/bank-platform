package com.bankplatform.common.event;

import java.time.Instant;
import java.util.UUID;

/** Matches plan/KAFKA.md's documented event envelope shape exactly. */
public record EventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        String correlationId,
        String causationId,
        String aggregateType,
        String aggregateId,
        String partitionKey,
        Object payload) {}
