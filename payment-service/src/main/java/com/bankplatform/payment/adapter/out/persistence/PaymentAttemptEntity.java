package com.bankplatform.payment.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttemptEntity {

    @Id private UUID id;

    @Column(name = "payment_instruction_id", nullable = false)
    private UUID paymentInstructionId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(nullable = false)
    private String status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected PaymentAttemptEntity() {}

    public PaymentAttemptEntity(
            UUID id,
            UUID paymentInstructionId,
            UUID transactionId,
            String status,
            String failureReason,
            Instant attemptedAt) {
        this.id = id;
        this.paymentInstructionId = paymentInstructionId;
        this.transactionId = transactionId;
        this.status = status;
        this.failureReason = failureReason;
        this.attemptedAt = attemptedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentInstructionId() {
        return paymentInstructionId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public String getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }
}
