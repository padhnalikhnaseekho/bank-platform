package com.bankplatform.reporting.domain;

import com.bankplatform.common.error.ConflictException;
import com.bankplatform.common.error.ValidationException;
import java.time.Instant;
import java.util.UUID;

public class StatementJob {

    private final StatementId id;
    private final UUID customerId;
    private final UUID accountId;
    private final Instant periodStart;
    private final Instant periodEnd;
    private StatementStatus status;
    private String csvFileUrl;
    private String pdfFileUrl;
    private final Instant createdAt;
    private Instant updatedAt;

    public StatementJob(StatementId id, UUID customerId, UUID accountId, Instant periodStart, Instant periodEnd,
            StatementStatus status, String csvFileUrl, String pdfFileUrl, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.accountId = accountId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.status = status;
        this.csvFileUrl = csvFileUrl;
        this.pdfFileUrl = pdfFileUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static StatementJob request(UUID customerId, UUID accountId, Instant periodStart, Instant periodEnd) {
        if (!periodStart.isBefore(periodEnd)) {
            throw new ValidationException("periodStart must be before periodEnd");
        }
        Instant now = Instant.now();
        return new StatementJob(StatementId.newId(), customerId, accountId, periodStart, periodEnd,
                StatementStatus.PENDING, null, null, now, now);
    }

    public void complete(String csvFileUrl, String pdfFileUrl) {
        requirePending();
        this.status = StatementStatus.COMPLETED;
        this.csvFileUrl = csvFileUrl;
        this.pdfFileUrl = pdfFileUrl;
        this.updatedAt = Instant.now();
    }

    public void fail() {
        requirePending();
        this.status = StatementStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    private void requirePending() {
        if (status != StatementStatus.PENDING) {
            throw new ConflictException("Statement job " + id + " must be PENDING but is " + status);
        }
    }

    public StatementId id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public UUID accountId() {
        return accountId;
    }

    public Instant periodStart() {
        return periodStart;
    }

    public Instant periodEnd() {
        return periodEnd;
    }

    public StatementStatus status() {
        return status;
    }

    public String csvFileUrl() {
        return csvFileUrl;
    }

    public String pdfFileUrl() {
        return pdfFileUrl;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
