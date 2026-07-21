package com.bankplatform.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void addsSameCurrency() {
        Money a = Money.of(new BigDecimal("10.00"), "INR");
        Money b = Money.of(new BigDecimal("5.50"), "INR");
        assertThat(a.add(b).amount()).isEqualByComparingTo("15.50");
    }

    @Test
    void subtractsSameCurrency() {
        Money a = Money.of(new BigDecimal("10.00"), "INR");
        Money b = Money.of(new BigDecimal("5.50"), "INR");
        assertThat(a.subtract(b).amount()).isEqualByComparingTo("4.50");
    }

    @Test
    void rejectsCurrencyMismatch() {
        Money inr = Money.of(BigDecimal.TEN, "INR");
        Money usd = Money.of(BigDecimal.ONE, "USD");
        assertThatThrownBy(() -> inr.add(usd)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detectsNegativeAndPositive() {
        assertThat(Money.of(new BigDecimal("-1"), "INR").isNegative()).isTrue();
        assertThat(Money.of(new BigDecimal("1"), "INR").isPositive()).isTrue();
        assertThat(Money.zero("INR").isPositive()).isFalse();
    }
}
