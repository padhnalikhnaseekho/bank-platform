package com.bankplatform.payment.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateRecurringPaymentRequest(
        @NotNull UUID sourceAccountId,
        @NotNull UUID payeeAccountId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank String currency,
        @NotNull Instant startAt,
        @NotNull @Min(1) Integer intervalDays) {}
