package com.bankplatform.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.payment.application.port.PaymentAttemptRepository;
import com.bankplatform.payment.application.port.PaymentInstructionRepository;
import com.bankplatform.payment.domain.Money;
import com.bankplatform.payment.domain.PaymentAttempt;
import com.bankplatform.payment.domain.PaymentInstruction;
import com.bankplatform.payment.domain.PaymentSchedule;
import com.bankplatform.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TriggerPaymentUseCaseTest {

    @Mock
    private PaymentInstructionRepository paymentInstructionRepository;

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private EventPublisher eventPublisher;

    private TriggerPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new TriggerPaymentUseCase(paymentInstructionRepository, paymentAttemptRepository, eventPublisher);
        when(paymentAttemptRepository.save(any(PaymentAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentInstructionRepository.save(any(PaymentInstruction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PaymentInstruction oneTimeInstruction() {
        return PaymentInstruction.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Money.of(new BigDecimal("100"), "INR"), PaymentSchedule.oneTime(Instant.now().minusSeconds(60)));
    }

    private PaymentInstruction recurringInstruction() {
        return PaymentInstruction.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Money.of(new BigDecimal("100"), "INR"),
                PaymentSchedule.recurring(Instant.now().minusSeconds(60), 30));
    }

    @Test
    void createsAnAttemptAndPublishesPaymentDueAndTransferStarted() {
        PaymentInstruction instruction = oneTimeInstruction();

        useCase.execute(instruction);

        verify(paymentAttemptRepository).save(any(PaymentAttempt.class));
        verify(eventPublisher).publish(eq("payment-due"), eq("PaymentInstruction"), eq(instruction.id().toString()),
                any());
        verify(eventPublisher).publish(eq("transfer-started"), eq("Transaction"), any(), any());
    }

    @Test
    void oneTimePaymentIsCompletedAfterItsSingleAttempt() {
        PaymentInstruction instruction = oneTimeInstruction();

        useCase.execute(instruction);

        assertThat(instruction.status()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void recurringPaymentAdvancesToItsNextRunAndStaysActive() {
        PaymentInstruction instruction = recurringInstruction();
        Instant originalNextRun = instruction.schedule().nextRunAt();

        useCase.execute(instruction);

        assertThat(instruction.status()).isEqualTo(PaymentStatus.ACTIVE);
        assertThat(instruction.schedule().nextRunAt()).isAfter(originalNextRun);
    }

    @Test
    void theTransactionIdUsedForTheAttemptMatchesTheOneInTheTransferStartedEvent() {
        PaymentInstruction instruction = oneTimeInstruction();
        ArgumentCaptor<PaymentAttempt> attemptCaptor = ArgumentCaptor.forClass(PaymentAttempt.class);
        ArgumentCaptor<Object> transferPayloadCaptor = ArgumentCaptor.forClass(Object.class);

        useCase.execute(instruction);

        verify(paymentAttemptRepository).save(attemptCaptor.capture());
        verify(eventPublisher, times(1)).publish(eq("transfer-started"), any(), any(),
                transferPayloadCaptor.capture());
        assertThat(transferPayloadCaptor.getValue().toString())
                .contains(attemptCaptor.getValue().transactionId().toString());
    }
}
