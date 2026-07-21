package com.bankplatform.payment.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_instructions")
public class PaymentInstructionEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "payee_account_id", nullable = false)
    private UUID payeeAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "schedule_type", nullable = false)
    private String scheduleType;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentInstructionEntity() {}

    public PaymentInstructionEntity(UUID id, UUID customerId, UUID sourceAccountId, UUID payeeAccountId,
            BigDecimal amount, String currency, String scheduleType, Integer intervalDays, Instant nextRunAt,
            String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.sourceAccountId = sourceAccountId;
        this.payeeAccountId = payeeAccountId;
        this.amount = amount;
        this.currency = currency;
        this.scheduleType = scheduleType;
        this.intervalDays = intervalDays;
        this.nextRunAt = nextRunAt;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getPayeeAccountId() {
        return payeeAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
