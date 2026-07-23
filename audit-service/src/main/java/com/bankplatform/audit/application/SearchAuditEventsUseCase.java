package com.bankplatform.audit.application;

import com.bankplatform.audit.application.port.AuditEventRepository;
import com.bankplatform.audit.domain.AuditEvent;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchAuditEventsUseCase {

    private final AuditEventRepository auditEventRepository;

    public SearchAuditEventsUseCase(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> search(
            String aggregateId, String eventType, Instant from, Instant to, Pageable pageable) {
        return auditEventRepository.search(aggregateId, eventType, from, to, pageable);
    }
}
