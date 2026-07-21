package com.bankplatform.payment.domain;

import com.bankplatform.common.error.ConflictException;
import com.bankplatform.common.error.ValidationException;
import java.time.Instant;
import java.util.UUID;

public class PaymentInstruction {

    private final PaymentId id;
    private final UUID customerId;
    private final UUID sourceAccountId;
    private final UUID payeeAccountId;
    private final Money amount;
    private PaymentSchedule schedule;
    private PaymentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public PaymentInstruction(PaymentId id, UUID customerId, UUID sourceAccountId, UUID payeeAccountId, Money amount,
            PaymentSchedule schedule, PaymentStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.sourceAccountId = sourceAccountId;
        this.payeeAccountId = payeeAccountId;
        this.amount = amount;
        this.schedule = schedule;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentInstruction create(UUID customerId, UUID sourceAccountId, UUID payeeAccountId, Money amount,
            PaymentSchedule schedule) {
        if (sourceAccountId.equals(payeeAccountId)) {
            throw new ValidationException("Source account and payee account must be different");
        }
        Instant now = Instant.now();
        return new PaymentInstruction(PaymentId.newId(), customerId, sourceAccountId, payeeAccountId, amount,
                schedule, PaymentStatus.ACTIVE, now, now);
    }

    public boolean isDue(Instant now) {
        return status == PaymentStatus.ACTIVE && !schedule.nextRunAt().isAfter(now);
    }

    /** Called once an attempt has been triggered for the current {@code nextRunAt}. */
    public void recordAttemptTriggered() {
        requireActive();
        if (schedule.type() == ScheduleType.ONE_TIME) {
            status = PaymentStatus.COMPLETED;
        } else {
            schedule = schedule.advance();
        }
        updatedAt = Instant.now();
    }

    public void cancel() {
        if (status != PaymentStatus.ACTIVE) {
            throw new ConflictException("Payment " + id + " must be ACTIVE to cancel but is " + status);
        }
        status = PaymentStatus.CANCELLED;
        updatedAt = Instant.now();
    }

    private void requireActive() {
        if (status != PaymentStatus.ACTIVE) {
            throw new ConflictException("Payment " + id + " must be ACTIVE but is " + status);
        }
    }

    public PaymentId id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public UUID sourceAccountId() {
        return sourceAccountId;
    }

    public UUID payeeAccountId() {
        return payeeAccountId;
    }

    public Money amount() {
        return amount;
    }

    public PaymentSchedule schedule() {
        return schedule;
    }

    public PaymentStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
