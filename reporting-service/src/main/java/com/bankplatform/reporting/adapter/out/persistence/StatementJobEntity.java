package com.bankplatform.reporting.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "statement_jobs")
public class StatementJobEntity {

    @Id private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(nullable = false)
    private String status;

    @Column(name = "csv_file_url")
    private String csvFileUrl;

    @Column(name = "pdf_file_url")
    private String pdfFileUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StatementJobEntity() {}

    public StatementJobEntity(
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

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public String getStatus() {
        return status;
    }

    public String getCsvFileUrl() {
        return csvFileUrl;
    }

    public String getPdfFileUrl() {
        return pdfFileUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
