package com.bankplatform.payment.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateScheduledPaymentRequest(
        @NotNull UUID sourceAccountId,
        @NotNull UUID payeeAccountId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank String currency,
        @NotNull @Future Instant runAt) {}
