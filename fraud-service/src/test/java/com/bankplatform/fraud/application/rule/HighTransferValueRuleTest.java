package com.bankplatform.fraud.application.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankplatform.fraud.domain.FraudAlertType;
import com.bankplatform.fraud.domain.TransferWindowStats;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HighTransferValueRuleTest {

    private final HighTransferValueRule rule = new HighTransferValueRule(new BigDecimal("50000"));

    @Test
    void doesNotAlertAtOrBelowTheThreshold() {
        TransferWindowStats stats = new TransferWindowStats(2, new BigDecimal("50000"), "INR");

        Optional<?> result = rule.evaluate("customer-1", stats, Instant.now(), Instant.now());

        assertThat(result).isEmpty();
    }

    @Test
    void alertsWhenTotalAmountExceedsTheThreshold() {
        TransferWindowStats stats = new TransferWindowStats(3, new BigDecimal("50000.01"), "INR");

        var result = rule.evaluate("customer-1", stats, Instant.now(), Instant.now());

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(FraudAlertType.HIGH_TRANSFER_VALUE);
        assertThat(result.get().totalAmount()).isEqualByComparingTo("50000.01");
    }
}
