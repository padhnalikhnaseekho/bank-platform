package com.bankplatform.payment.application.event;

/** Published as payment-success or payment-failed once the underlying transfer's outcome is known. */
public record PaymentOutcomePayload(
        String paymentId,
        String attemptId,
        String transactionId,
        String status,
        String failureReason) {}
