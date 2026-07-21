package com.bankplatform.fraud.application.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankplatform.fraud.domain.FraudAlertType;
import com.bankplatform.fraud.domain.TransferWindowStats;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HighTransferCountRuleTest {

    private final HighTransferCountRule rule = new HighTransferCountRule(5);

    @Test
    void doesNotAlertAtOrBelowTheThreshold() {
        TransferWindowStats stats = new TransferWindowStats(5, new BigDecimal("100"), "INR");

        Optional<?> result = rule.evaluate("customer-1", stats, Instant.now(), Instant.now());

        assertThat(result).isEmpty();
    }

    @Test
    void alertsWhenCountExceedsTheThreshold() {
        TransferWindowStats stats = new TransferWindowStats(6, new BigDecimal("100"), "INR");
        Instant start = Instant.now().minusSeconds(600);
        Instant end = Instant.now();

        var result = rule.evaluate("customer-1", stats, start, end);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(FraudAlertType.HIGH_TRANSFER_COUNT);
        assertThat(result.get().customerId()).isEqualTo("customer-1");
        assertThat(result.get().transferCount()).isEqualTo(6);
        assertThat(result.get().windowStart()).isEqualTo(start);
        assertThat(result.get().windowEnd()).isEqualTo(end);
    }
}
