package com.bankplatform.fraud.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransferWindowStatsTest {

    @Test
    void zeroStartsAtNoTransfersAndNoAmount() {
        TransferWindowStats stats = TransferWindowStats.zero();

        assertThat(stats.count()).isZero();
        assertThat(stats.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.currency()).isNull();
    }

    @Test
    void plusAccumulatesCountAmountAndTracksTheLatestCurrency() {
        TransferWindowStats stats =
                TransferWindowStats.zero()
                        .plus(new BigDecimal("100"), "INR")
                        .plus(new BigDecimal("50"), "INR");

        assertThat(stats.count()).isEqualTo(2);
        assertThat(stats.totalAmount()).isEqualByComparingTo("150");
        assertThat(stats.currency()).isEqualTo("INR");
    }
}
