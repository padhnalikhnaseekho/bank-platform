package com.bankplatform.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankplatform.common.error.ConflictException;
import com.bankplatform.common.error.ValidationException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentInstructionTest {

    private final Money amount = Money.of(new BigDecimal("100"), "INR");

    @Test
    void rejectsSourceAndPayeeBeingTheSameAccount() {
        UUID accountId = UUID.randomUUID();

        assertThatThrownBy(() -> PaymentInstruction.create(UUID.randomUUID(), accountId, accountId, amount,
                PaymentSchedule.oneTime(Instant.now()))).isInstanceOf(ValidationException.class);
    }

    @Test
    void isDueOnlyWhenActiveAndNextRunAtHasPassed() {
        Instant past = Instant.now().minusSeconds(60);
        PaymentInstruction instruction = PaymentInstruction.create(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), amount, PaymentSchedule.oneTime(past));

        assertThat(instruction.isDue(Instant.now())).isTrue();
    }

    @Test
    void isNotDueBeforeNextRunAt() {
        Instant future = Instant.now().plusSeconds(3600);
        PaymentInstruction instruction = PaymentInstruction.create(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), amount, PaymentSchedule.oneTime(future));

        assertThat(instruction.isDue(Instant.now())).isFalse();
    }

    @Test
    void oneTimePaymentCompletesAfterItsAttemptIsTriggered() {
        PaymentInstruction instruction = PaymentInstruction.create(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), amount, PaymentSchedule.oneTime(Instant.now()));

        instruction.recordAttemptTriggered();

        assertThat(instruction.status()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void recurringPaymentStaysActiveAndAdvancesToItsNextOccurrence() {
        Instant firstRun = Instant.now().minusSeconds(60);
        PaymentInstruction instruction = PaymentInstruction.create(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), amount, PaymentSchedule.recurring(firstRun, 30));

        instruction.recordAttemptTriggered();

        assertThat(instruction.status()).isEqualTo(PaymentStatus.ACTIVE);
        assertThat(instruction.schedule().nextRunAt()).isEqualTo(firstRun.plus(Duration.ofDays(30)));
    }

    @Test
    void cancelMarksAnActivePaymentCancelled() {
        PaymentInstruction instruction = PaymentInstruction.create(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), amount, PaymentSchedule.oneTime(Instant.now()));

        instruction.cancel();

        assertThat(instruction.status()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void rejectsCancellingAnAlreadyCancelledPayment() {
        PaymentInstruction instruction = PaymentInstruction.create(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), amount, PaymentSchedule.oneTime(Instant.now()));
        instruction.cancel();

        assertThatThrownBy(instruction::cancel).isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsCancellingACompletedPayment() {
        PaymentInstruction instruction = PaymentInstruction.create(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), amount, PaymentSchedule.oneTime(Instant.now()));
        instruction.recordAttemptTriggered();

        assertThatThrownBy(instruction::cancel).isInstanceOf(ConflictException.class);
    }
}
