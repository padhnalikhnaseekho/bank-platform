package com.bankplatform.payment.application.event;

public record PaymentCancelledPayload(String paymentId, String customerId) {}
