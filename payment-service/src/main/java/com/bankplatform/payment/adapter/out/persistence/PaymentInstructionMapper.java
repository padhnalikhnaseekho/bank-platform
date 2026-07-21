package com.bankplatform.payment.adapter.out.persistence;

import com.bankplatform.payment.domain.Money;
import com.bankplatform.payment.domain.PaymentId;
import com.bankplatform.payment.domain.PaymentInstruction;
import com.bankplatform.payment.domain.PaymentSchedule;
import com.bankplatform.payment.domain.PaymentStatus;
import com.bankplatform.payment.domain.ScheduleType;
import java.util.Currency;

final class PaymentInstructionMapper {

    private PaymentInstructionMapper() {}

    static PaymentInstruction toDomain(PaymentInstructionEntity entity) {
        Money amount = new Money(entity.getAmount(), Currency.getInstance(entity.getCurrency()));
        PaymentSchedule schedule = new PaymentSchedule(ScheduleType.valueOf(entity.getScheduleType()),
                entity.getNextRunAt(), entity.getIntervalDays());
        return new PaymentInstruction(PaymentId.of(entity.getId()), entity.getCustomerId(),
                entity.getSourceAccountId(), entity.getPayeeAccountId(), amount, schedule,
                PaymentStatus.valueOf(entity.getStatus()), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    static PaymentInstructionEntity toEntity(PaymentInstruction instruction) {
        return new PaymentInstructionEntity(instruction.id().value(), instruction.customerId(),
                instruction.sourceAccountId(), instruction.payeeAccountId(), instruction.amount().amount(),
                instruction.amount().currency().getCurrencyCode(), instruction.schedule().type().name(),
                instruction.schedule().intervalDays(), instruction.schedule().nextRunAt(),
                instruction.status().name(), instruction.createdAt(), instruction.updatedAt());
    }
}
