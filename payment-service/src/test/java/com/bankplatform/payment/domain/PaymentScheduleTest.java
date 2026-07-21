package com.bankplatform.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PaymentScheduleTest {

    @Test
    void oneTimeScheduleHasNoInterval() {
        Instant runAt = Instant.now();

        PaymentSchedule schedule = PaymentSchedule.oneTime(runAt);

        assertThat(schedule.type()).isEqualTo(ScheduleType.ONE_TIME);
        assertThat(schedule.nextRunAt()).isEqualTo(runAt);
        assertThat(schedule.intervalDays()).isNull();
    }

    @Test
    void recurringScheduleRequiresAPositiveInterval() {
        assertThatThrownBy(() -> PaymentSchedule.recurring(Instant.now(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void oneTimeScheduleRejectsAnInterval() {
        assertThatThrownBy(() -> new PaymentSchedule(ScheduleType.ONE_TIME, Instant.now(), 7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void advanceMovesNextRunAtForwardByTheInterval() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        PaymentSchedule schedule = PaymentSchedule.recurring(start, 30);

        PaymentSchedule advanced = schedule.advance();

        assertThat(advanced.nextRunAt()).isEqualTo(start.plus(Duration.ofDays(30)));
        assertThat(advanced.intervalDays()).isEqualTo(30);
    }
}
