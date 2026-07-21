package com.bankplatform.fraud.domain;

import java.math.BigDecimal;

/**
 * Running count and outgoing total for one customer within one sliding window. Assumes a
 * single currency per customer (this platform is INR-only in practice) — {@code currency}
 * simply tracks the most recently seen value rather than modeling mixed-currency totals.
 */
public record TransferWindowStats(long count, BigDecimal totalAmount, String currency) {

    public static TransferWindowStats zero() {
        return new TransferWindowStats(0, BigDecimal.ZERO, null);
    }

    public TransferWindowStats plus(BigDecimal amount, String currency) {
        return new TransferWindowStats(count + 1, totalAmount.add(amount), currency);
    }
}
