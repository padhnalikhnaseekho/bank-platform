package com.bankplatform.audit.application;

import com.bankplatform.audit.application.port.AuditEventRepository;
import com.bankplatform.audit.domain.AuditEvent;
import com.bankplatform.common.event.EventEnvelope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordAuditEventUseCase {

    private final AuditEventRepository auditEventRepository;

    public RecordAuditEventUseCase(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void record(EventEnvelope envelope, String payloadJson) {
        AuditEvent event =
                AuditEvent.capture(
                        envelope.eventId(),
                        envelope.eventType(),
                        envelope.aggregateType(),
                        envelope.aggregateId(),
                        payloadJson,
                        envelope.correlationId(),
                        envelope.occurredAt());
        auditEventRepository.save(event);
    }
}
