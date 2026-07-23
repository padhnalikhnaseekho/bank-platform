package com.bankplatform.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.payment.application.port.PaymentAttemptRepository;
import com.bankplatform.payment.domain.PaymentAttempt;
import com.bankplatform.payment.domain.PaymentAttemptStatus;
import com.bankplatform.payment.domain.PaymentId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplyPaymentOutcomeUseCaseTest {

    @Mock private PaymentAttemptRepository paymentAttemptRepository;

    @Mock private EventPublisher eventPublisher;

    private ApplyPaymentOutcomeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ApplyPaymentOutcomeUseCase(paymentAttemptRepository, eventPublisher);
    }

    @Test
    void marksTheMatchingAttemptSucceededAndPublishesPaymentSuccess() {
        UUID transactionId = UUID.randomUUID();
        PaymentAttempt attempt = PaymentAttempt.create(PaymentId.newId(), transactionId);
        when(paymentAttemptRepository.findByTransactionId(transactionId))
                .thenReturn(Optional.of(attempt));

        useCase.execute(transactionId, true, null);

        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.SUCCEEDED);
        verify(paymentAttemptRepository).save(attempt);
        verify(eventPublisher)
                .publish(
                        eq("payment-success"),
                        eq("PaymentAttempt"),
                        eq(attempt.id().toString()),
                        any());
    }

    @Test
    void marksTheMatchingAttemptFailedAndPublishesPaymentFailed() {
        UUID transactionId = UUID.randomUUID();
        PaymentAttempt attempt = PaymentAttempt.create(PaymentId.newId(), transactionId);
        when(paymentAttemptRepository.findByTransactionId(transactionId))
                .thenReturn(Optional.of(attempt));

        useCase.execute(transactionId, false, "Insufficient funds");

        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(attempt.failureReason()).isEqualTo("Insufficient funds");
        verify(eventPublisher)
                .publish(
                        eq("payment-failed"),
                        eq("PaymentAttempt"),
                        eq(attempt.id().toString()),
                        any());
    }

    @Test
    void ignoresTransferOutcomesThatDoNotBelongToAnyPaymentAttempt() {
        UUID transactionId = UUID.randomUUID();
        when(paymentAttemptRepository.findByTransactionId(transactionId))
                .thenReturn(Optional.empty());

        useCase.execute(transactionId, true, null);

        verify(paymentAttemptRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any(), any(), any());
    }
}
