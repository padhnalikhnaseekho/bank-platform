package com.bankplatform.payment.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.common.event.IdempotentEventProcessor;
import com.bankplatform.payment.application.port.PaymentAttemptRepository;
import com.bankplatform.payment.domain.PaymentAttempt;
import com.bankplatform.payment.domain.PaymentAttemptStatus;
import com.bankplatform.payment.domain.PaymentId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentOutcomeListenerTest {

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private IdempotentEventProcessor idempotentEventProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PaymentOutcomeListener listener;

    private record TransferOutcomePayload(String transactionId, String sourceAccountId, String targetAccountId,
            String sourceCustomerId, String targetCustomerId, BigDecimal amount, String currency,
            String failureReason) {}

    @BeforeEach
    void setUp() {
        listener = new PaymentOutcomeListener(paymentAttemptRepository, eventPublisher, idempotentEventProcessor,
                objectMapper);
        doAnswer(invocation -> {
            Runnable handler = invocation.getArgument(2);
            handler.run();
            return null;
        }).when(idempotentEventProcessor).process(any(), any(), any());
    }

    private String toMessage(String eventType, UUID transactionId, String failureReason) {
        TransferOutcomePayload payload = new TransferOutcomePayload(transactionId.toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), new BigDecimal("50.00"), "INR", failureReason);
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), eventType, 1, Instant.now(), "account-service",
                "corr-1", null, "Transfer", transactionId.toString(), transactionId.toString(), payload);
        return objectMapper.writeValueAsString(envelope);
    }

    @Test
    void marksTheMatchingAttemptSucceededAndPublishesPaymentSuccess() {
        UUID transactionId = UUID.randomUUID();
        PaymentAttempt attempt = PaymentAttempt.create(PaymentId.newId(), transactionId);
        when(paymentAttemptRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(attempt));

        listener.onTransferCompleted(toMessage("transfer-completed", transactionId, null));

        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.SUCCEEDED);
        verify(paymentAttemptRepository).save(attempt);
        verify(eventPublisher).publish(eq("payment-success"), eq("PaymentAttempt"), eq(attempt.id().toString()),
                any());
    }

    @Test
    void marksTheMatchingAttemptFailedAndPublishesPaymentFailed() {
        UUID transactionId = UUID.randomUUID();
        PaymentAttempt attempt = PaymentAttempt.create(PaymentId.newId(), transactionId);
        when(paymentAttemptRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(attempt));

        listener.onTransferFailed(toMessage("transfer-failed", transactionId, "Insufficient funds"));

        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(attempt.failureReason()).isEqualTo("Insufficient funds");
        verify(eventPublisher).publish(eq("payment-failed"), eq("PaymentAttempt"), eq(attempt.id().toString()),
                any());
    }

    @Test
    void ignoresTransferOutcomesThatDoNotBelongToAnyPaymentAttempt() {
        UUID transactionId = UUID.randomUUID();
        when(paymentAttemptRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

        listener.onTransferCompleted(toMessage("transfer-completed", transactionId, null));

        verify(paymentAttemptRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any(), any(), any());
    }
}
