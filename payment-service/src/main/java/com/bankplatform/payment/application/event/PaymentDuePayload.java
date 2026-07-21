package com.bankplatform.payment.application.event;

import java.math.BigDecimal;

/** Published when a payment's scheduled run time has arrived, before the transfer is triggered. */
public record PaymentDuePayload(
        String paymentId,
        String attemptId,
        String transactionId,
        String sourceAccountId,
        String payeeAccountId,
        BigDecimal amount,
        String currency) {}
