package com.bankplatform.account.adapter.in.web.dto;

import com.bankplatform.account.domain.Account;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(UUID id, UUID customerId, String accountNumber, String type, String status,
        BigDecimal balance, String currency, Instant createdAt, Instant updatedAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.id().value(), account.customerId(), account.accountNumber(),
                account.type().name(), account.status().name(), account.balance().amount(),
                account.balance().currency().getCurrencyCode(), account.createdAt(), account.updatedAt());
    }
}
