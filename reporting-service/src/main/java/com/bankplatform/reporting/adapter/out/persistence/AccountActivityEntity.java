package com.bankplatform.reporting.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_activity_view")
public class AccountActivityEntity {

    @Id private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AccountActivityEntity() {}

    public AccountActivityEntity(
            UUID id,
            UUID customerId,
            UUID accountId,
            String eventType,
            BigDecimal amount,
            String currency,
            Instant occurredAt) {
        this.id = id;
        this.customerId = customerId;
        this.accountId = accountId;
        this.eventType = eventType;
        this.amount = amount;
        this.currency = currency;
        this.occurredAt = occurredAt;
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

    public String getEventType() {
        return eventType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
