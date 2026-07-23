package com.bankplatform.account.application;

import com.bankplatform.account.domain.Account;
import java.math.BigDecimal;

public record AccountEventPayload(
        String accountId,
        String customerId,
        String accountNumber,
        String type,
        String status,
        BigDecimal balance,
        String currency) {

    public static AccountEventPayload from(Account account) {
        return new AccountEventPayload(
                account.id().toString(),
                account.customerId().toString(),
                account.accountNumber(),
                account.type().name(),
                account.status().name(),
                account.balance().amount(),
                account.balance().currency().getCurrencyCode());
    }
}
