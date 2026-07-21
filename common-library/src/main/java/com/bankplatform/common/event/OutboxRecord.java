package com.bankplatform.common.event;

import java.time.Instant;
import java.util.UUID;

public record OutboxRecord(
        UUID id,
        String aggregateType,
        String aggregateId,
        String eventType,
        int eventVersion,
        String payloadJson,
        String correlationId,
        Instant createdAt) {}
