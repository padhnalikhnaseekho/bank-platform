package com.bankplatform.reporting.adapter.in.web.dto;

import com.bankplatform.reporting.domain.StatementJob;
import java.time.Instant;
import java.util.UUID;

public record StatementJobResponse(
        UUID id,
        UUID customerId,
        UUID accountId,
        Instant periodStart,
        Instant periodEnd,
        String status,
        String csvFileUrl,
        String pdfFileUrl,
        Instant createdAt,
        Instant updatedAt) {

    public static StatementJobResponse from(StatementJob job) {
        return new StatementJobResponse(
                job.id().value(),
                job.customerId(),
                job.accountId(),
                job.periodStart(),
                job.periodEnd(),
                job.status().name(),
                job.csvFileUrl(),
                job.pdfFileUrl(),
                job.createdAt(),
                job.updatedAt());
    }
}
