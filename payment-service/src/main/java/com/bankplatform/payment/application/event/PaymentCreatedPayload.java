package com.bankplatform.payment.application.event;

import com.bankplatform.payment.domain.PaymentInstruction;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCreatedPayload(
        String paymentId,
        String customerId,
        String sourceAccountId,
        String payeeAccountId,
        BigDecimal amount,
        String currency,
        String scheduleType,
        Instant nextRunAt) {

    public static PaymentCreatedPayload from(PaymentInstruction instruction) {
        return new PaymentCreatedPayload(instruction.id().toString(), instruction.customerId().toString(),
                instruction.sourceAccountId().toString(), instruction.payeeAccountId().toString(),
                instruction.amount().amount(), instruction.amount().currency().getCurrencyCode(),
                instruction.schedule().type().name(), instruction.schedule().nextRunAt());
    }
}
