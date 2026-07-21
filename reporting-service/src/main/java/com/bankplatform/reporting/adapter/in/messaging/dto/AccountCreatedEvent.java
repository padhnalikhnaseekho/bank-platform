package com.bankplatform.reporting.adapter.in.messaging.dto;

import java.math.BigDecimal;

/** Wire shape published by Account Service on account-created. */
public record AccountCreatedEvent(
        String accountId,
        String customerId,
        String accountNumber,
        String type,
        String status,
        BigDecimal balance,
        String currency) {}
