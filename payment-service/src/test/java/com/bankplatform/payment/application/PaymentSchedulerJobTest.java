package com.bankplatform.payment.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.payment.application.port.PaymentInstructionRepository;
import com.bankplatform.payment.domain.Money;
import com.bankplatform.payment.domain.PaymentInstruction;
import com.bankplatform.payment.domain.PaymentSchedule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentSchedulerJobTest {

    @Mock
    private PaymentInstructionRepository paymentInstructionRepository;

    @Mock
    private TriggerPaymentUseCase triggerPaymentUseCase;

    private PaymentSchedulerJob job;

    @BeforeEach
    void setUp() {
        job = new PaymentSchedulerJob(paymentInstructionRepository, triggerPaymentUseCase);
    }

    private PaymentInstruction dueInstruction() {
        return PaymentInstruction.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Money.of(new BigDecimal("100"), "INR"), PaymentSchedule.oneTime(Instant.now().minusSeconds(60)));
    }

    @Test
    void triggersEachDuePaymentThroughTheUseCase() {
        PaymentInstruction first = dueInstruction();
        PaymentInstruction second = dueInstruction();
        when(paymentInstructionRepository.findDue(any(Instant.class), anyInt())).thenReturn(List.of(first, second));

        job.processDuePayments();

        verify(triggerPaymentUseCase).execute(first);
        verify(triggerPaymentUseCase).execute(second);
    }

    @Test
    void doesNothingWhenNoPaymentsAreDue() {
        when(paymentInstructionRepository.findDue(any(Instant.class), anyInt())).thenReturn(List.of());

        job.processDuePayments();

        verify(triggerPaymentUseCase, never()).execute(any());
    }
}
