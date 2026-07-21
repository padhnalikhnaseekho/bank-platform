package com.bankplatform.audit.adapter.in.web;

import com.bankplatform.audit.adapter.in.web.dto.AuditEventListResponse;
import com.bankplatform.audit.application.SearchAuditEventsUseCase;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final SearchAuditEventsUseCase searchAuditEventsUseCase;

    public AuditController(SearchAuditEventsUseCase searchAuditEventsUseCase) {
        this.searchAuditEventsUseCase = searchAuditEventsUseCase;
    }

    @GetMapping("/events")
    @PreAuthorize("hasRole('ADMIN')")
    public AuditEventListResponse search(@RequestParam(required = false) String aggregateId,
            @RequestParam(required = false) String eventType, @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return AuditEventListResponse.from(
                searchAuditEventsUseCase.search(aggregateId, eventType, from, to, PageRequest.of(page, size)));
    }
}
