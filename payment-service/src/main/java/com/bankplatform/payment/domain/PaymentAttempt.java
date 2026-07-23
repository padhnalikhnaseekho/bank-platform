package com.bankplatform.payment.domain;

import com.bankplatform.common.error.ConflictException;
import java.time.Instant;
import java.util.UUID;

public class PaymentAttempt {

    private final PaymentAttemptId id;
    private final PaymentId paymentInstructionId;
    private final UUID transactionId;
    private PaymentAttemptStatus status;
    private String failureReason;
    private final Instant attemptedAt;

    public PaymentAttempt(
            PaymentAttemptId id,
            PaymentId paymentInstructionId,
            UUID transactionId,
            PaymentAttemptStatus status,
            String failureReason,
            Instant attemptedAt) {
        this.id = id;
        this.paymentInstructionId = paymentInstructionId;
        this.transactionId = transactionId;
        this.status = status;
        this.failureReason = failureReason;
        this.attemptedAt = attemptedAt;
    }

    public static PaymentAttempt create(PaymentId paymentInstructionId, UUID transactionId) {
        return new PaymentAttempt(
                PaymentAttemptId.newId(),
                paymentInstructionId,
                transactionId,
                PaymentAttemptStatus.PENDING,
                null,
                Instant.now());
    }

    public void markSucceeded() {
        requirePending();
        status = PaymentAttemptStatus.SUCCEEDED;
    }

    public void markFailed(String reason) {
        requirePending();
        status = PaymentAttemptStatus.FAILED;
        failureReason = reason;
    }

    private void requirePending() {
        if (status != PaymentAttemptStatus.PENDING) {
            throw new ConflictException(
                    "Payment attempt " + id + " must be PENDING but is " + status);
        }
    }

    public PaymentAttemptId id() {
        return id;
    }

    public PaymentId paymentInstructionId() {
        return paymentInstructionId;
    }

    public UUID transactionId() {
        return transactionId;
    }

    public PaymentAttemptStatus status() {
        return status;
    }

    public String failureReason() {
        return failureReason;
    }

    public Instant attemptedAt() {
        return attemptedAt;
    }
}
