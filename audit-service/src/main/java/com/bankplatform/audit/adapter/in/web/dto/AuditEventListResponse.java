package com.bankplatform.audit.adapter.in.web.dto;

import com.bankplatform.audit.domain.AuditEvent;
import java.util.List;
import org.springframework.data.domain.Page;

public record AuditEventListResponse(
        List<AuditEventResponse> items, int page, int size, long totalElements, int totalPages) {

    public static AuditEventListResponse from(Page<AuditEvent> page) {
        return new AuditEventListResponse(
                page.getContent().stream().map(AuditEventResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
