package com.bankplatform.payment.adapter.in.web.dto;

import com.bankplatform.payment.domain.PaymentInstruction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentInstructionResponse(
        UUID id,
        UUID customerId,
        UUID sourceAccountId,
        UUID payeeAccountId,
        BigDecimal amount,
        String currency,
        String scheduleType,
        Integer intervalDays,
        Instant nextRunAt,
        String status,
        Instant createdAt,
        Instant updatedAt) {

    public static PaymentInstructionResponse from(PaymentInstruction instruction) {
        return new PaymentInstructionResponse(instruction.id().value(), instruction.customerId(),
                instruction.sourceAccountId(), instruction.payeeAccountId(), instruction.amount().amount(),
                instruction.amount().currency().getCurrencyCode(), instruction.schedule().type().name(),
                instruction.schedule().intervalDays(), instruction.schedule().nextRunAt(),
                instruction.status().name(), instruction.createdAt(), instruction.updatedAt());
    }
}
