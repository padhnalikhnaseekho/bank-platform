package com.bankplatform.payment.domain;

import java.time.Duration;
import java.time.Instant;

public record PaymentSchedule(ScheduleType type, Instant nextRunAt, Integer intervalDays) {

    public PaymentSchedule {
        if (nextRunAt == null) {
            throw new IllegalArgumentException("nextRunAt must not be null");
        }
        if (type == ScheduleType.RECURRING && (intervalDays == null || intervalDays <= 0)) {
            throw new IllegalArgumentException("Recurring payments require a positive intervalDays");
        }
        if (type == ScheduleType.ONE_TIME && intervalDays != null) {
            throw new IllegalArgumentException("One-time payments must not have an intervalDays");
        }
    }

    public static PaymentSchedule oneTime(Instant runAt) {
        return new PaymentSchedule(ScheduleType.ONE_TIME, runAt, null);
    }

    public static PaymentSchedule recurring(Instant startAt, int intervalDays) {
        return new PaymentSchedule(ScheduleType.RECURRING, startAt, intervalDays);
    }

    /** Only valid for {@link ScheduleType#RECURRING} — advances by the fixed interval, not to "now". */
    public PaymentSchedule advance() {
        return new PaymentSchedule(type, nextRunAt.plus(Duration.ofDays(intervalDays)), intervalDays);
    }
}
