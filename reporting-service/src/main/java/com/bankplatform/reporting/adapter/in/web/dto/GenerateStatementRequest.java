package com.bankplatform.reporting.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record GenerateStatementRequest(
        @NotNull UUID accountId, @NotNull Instant periodStart, @NotNull Instant periodEnd) {}
