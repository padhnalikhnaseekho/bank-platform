package com.bankplatform.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankplatform.common.error.ConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentAttemptTest {

    @Test
    void createsAPendingAttempt() {
        PaymentId paymentId = PaymentId.newId();
        UUID transactionId = UUID.randomUUID();

        PaymentAttempt attempt = PaymentAttempt.create(paymentId, transactionId);

        assertThat(attempt.paymentInstructionId()).isEqualTo(paymentId);
        assertThat(attempt.transactionId()).isEqualTo(transactionId);
        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.PENDING);
    }

    @Test
    void marksSucceeded() {
        PaymentAttempt attempt = PaymentAttempt.create(PaymentId.newId(), UUID.randomUUID());

        attempt.markSucceeded();

        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.SUCCEEDED);
    }

    @Test
    void marksFailedWithAReason() {
        PaymentAttempt attempt = PaymentAttempt.create(PaymentId.newId(), UUID.randomUUID());

        attempt.markFailed("Insufficient funds");

        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(attempt.failureReason()).isEqualTo("Insufficient funds");
    }

    @Test
    void rejectsResolvingAnAlreadyResolvedAttempt() {
        PaymentAttempt attempt = PaymentAttempt.create(PaymentId.newId(), UUID.randomUUID());
        attempt.markSucceeded();

        assertThatThrownBy(attempt::markSucceeded).isInstanceOf(ConflictException.class);
    }
}
