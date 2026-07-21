package com.bankplatform.audit.application.port;

import com.bankplatform.audit.domain.AuditEvent;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditEventRepository {

    void save(AuditEvent event);

    Page<AuditEvent> search(String aggregateId, String eventType, Instant from, Instant to, Pageable pageable);
}
