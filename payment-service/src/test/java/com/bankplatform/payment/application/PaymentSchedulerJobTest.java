package com.bankplatform.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentSchedulerJobTest {

    @Mock
    private PaymentInstructionRepository paymentInstructionRepository;

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private EventPublisher eventPublisher;

    private PaymentSchedulerJob job;

    @BeforeEach
    void setUp() {
        job = new PaymentSchedulerJob(paymentInstructionRepository, paymentAttemptRepository, eventPublisher);
        when(paymentAttemptRepository.save(any(PaymentAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentInstructionRepository.save(any(PaymentInstruction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PaymentInstruction dueOneTimeInstruction() {
        return PaymentInstruction.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Money.of(new BigDecimal("100"), "INR"), PaymentSchedule.oneTime(Instant.now().minusSeconds(60)));
    }

    private PaymentInstruction dueRecurringInstruction() {
        return PaymentInstruction.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Money.of(new BigDecimal("100"), "INR"),
                PaymentSchedule.recurring(Instant.now().minusSeconds(60), 30));
    }

    @Test
    void createsAnAttemptAndPublishesPaymentDueAndTransferStartedForEachDuePayment() {
        PaymentInstruction instruction = dueOneTimeInstruction();
        when(paymentInstructionRepository.findDue(any(Instant.class), anyInt())).thenReturn(List.of(instruction));

        job.processDuePayments();

        verify(paymentAttemptRepository).save(any(PaymentAttempt.class));
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.eq("payment-due"),
                org.mockito.ArgumentMatchers.eq("PaymentInstruction"),
                org.mockito.ArgumentMatchers.eq(instruction.id().toString()), any());
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.eq("transfer-started"),
                org.mockito.ArgumentMatchers.eq("Transaction"), any(), any());
    }

    @Test
    void oneTimePaymentIsCompletedAfterItsSingleAttempt() {
        PaymentInstruction instruction = dueOneTimeInstruction();
        when(paymentInstructionRepository.findDue(any(Instant.class), anyInt())).thenReturn(List.of(instruction));

        job.processDuePayments();

        assertThat(instruction.status()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void recurringPaymentAdvancesToItsNextRunAndStaysActive() {
        PaymentInstruction instruction = dueRecurringInstruction();
        Instant originalNextRun = instruction.schedule().nextRunAt();
        when(paymentInstructionRepository.findDue(any(Instant.class), anyInt())).thenReturn(List.of(instruction));

        job.processDuePayments();

        assertThat(instruction.status()).isEqualTo(PaymentStatus.ACTIVE);
        assertThat(instruction.schedule().nextRunAt()).isAfter(originalNextRun);
    }

    @Test
    void theTransactionIdUsedForTheAttemptMatchesTheOneInTheTransferStartedEvent() {
        PaymentInstruction instruction = dueOneTimeInstruction();
        when(paymentInstructionRepository.findDue(any(Instant.class), anyInt())).thenReturn(List.of(instruction));
        ArgumentCaptor<PaymentAttempt> attemptCaptor = ArgumentCaptor.forClass(PaymentAttempt.class);
        ArgumentCaptor<Object> transferPayloadCaptor = ArgumentCaptor.forClass(Object.class);

        job.processDuePayments();

        verify(paymentAttemptRepository).save(attemptCaptor.capture());
        verify(eventPublisher, times(1)).publish(org.mockito.ArgumentMatchers.eq("transfer-started"), any(), any(),
                transferPayloadCaptor.capture());
        assertThat(transferPayloadCaptor.getValue().toString())
                .contains(attemptCaptor.getValue().transactionId().toString());
    }
}
