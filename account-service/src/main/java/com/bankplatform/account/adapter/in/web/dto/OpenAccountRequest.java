package com.bankplatform.account.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record OpenAccountRequest(@NotBlank String type, @NotBlank String currency) {}
