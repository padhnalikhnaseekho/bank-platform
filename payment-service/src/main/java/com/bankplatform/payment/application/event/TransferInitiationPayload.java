package com.bankplatform.payment.application.event;

import java.math.BigDecimal;

/**
 * Published onto the same {@code transfer-started} topic and wire shape that Transaction Service
 * uses (see account-service's TransferListener) — Account Service is the sole owner of balance
 * mutations, and a transfer is a transfer regardless of whether a customer requested it directly or
 * a due payment schedule triggered it.
 */
public record TransferInitiationPayload(
        String transactionId,
        String type,
        String sourceAccountId,
        String targetAccountId,
        BigDecimal amount,
        String currency) {}
