package com.bankplatform.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankplatform.common.error.ConflictException;
import com.bankplatform.common.error.ValidationException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    void creditIncreasesBalance() {
        Account account = Account.open(UUID.randomUUID(), "123456789012", AccountType.SAVINGS, "INR");
        account.credit(Money.of(new BigDecimal("100.00"), "INR"), "ref-1");
        assertThat(account.balance().amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void debitDecreasesBalance() {
        Account account = Account.open(UUID.randomUUID(), "123456789012", AccountType.SAVINGS, "INR");
        account.credit(Money.of(new BigDecimal("100.00"), "INR"), "ref-1");
        account.debit(Money.of(new BigDecimal("40.00"), "INR"), "ref-2");
        assertThat(account.balance().amount()).isEqualByComparingTo("60.00");
    }

    @Test
    void debitBeyondBalanceIsRejected() {
        Account account = Account.open(UUID.randomUUID(), "123456789012", AccountType.SAVINGS, "INR");
        Money amount = Money.of(new BigDecimal("10.00"), "INR");
        assertThatThrownBy(() -> account.debit(amount, "ref-1")).isInstanceOf(ConflictException.class);
    }

    @Test
    void nonPositiveAmountIsRejected() {
        Account account = Account.open(UUID.randomUUID(), "123456789012", AccountType.SAVINGS, "INR");
        Money zero = Money.zero("INR");
        assertThatThrownBy(() -> account.credit(zero, "ref-1")).isInstanceOf(ValidationException.class);
    }

    @Test
    void frozenAccountRejectsDebitAndCredit() {
        Account account = Account.open(UUID.randomUUID(), "123456789012", AccountType.SAVINGS, "INR");
        account.freeze();
        Money amount = Money.of(BigDecimal.ONE, "INR");
        assertThatThrownBy(() -> account.credit(amount, "ref-1")).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> account.debit(amount, "ref-2")).isInstanceOf(ConflictException.class);
    }

    @Test
    void closedAccountCannotBeFrozen() {
        Account account = Account.open(UUID.randomUUID(), "123456789012", AccountType.SAVINGS, "INR");
        account.close();
        assertThatThrownBy(account::freeze).isInstanceOf(ConflictException.class);
    }
}
