package com.bankplatform.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void createsMoneyFromAmountAndCurrencyCode() {
        Money money = Money.of(new BigDecimal("10.50"), "INR");

        assertThat(money.amount()).isEqualByComparingTo("10.50");
        assertThat(money.currency().getCurrencyCode()).isEqualTo("INR");
    }

    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> Money.of(BigDecimal.ZERO, "INR")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("-5"), "INR"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> new Money(null, java.util.Currency.getInstance("INR")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
