package com.bankplatform.payment.application;

import com.bankplatform.payment.application.port.PaymentInstructionRepository;
import com.bankplatform.payment.domain.PaymentInstruction;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls for due payments and triggers each one via {@link TriggerPaymentUseCase}. */
@Component
public class PaymentSchedulerJob {

    private static final int BATCH_SIZE = 50;

    private final PaymentInstructionRepository paymentInstructionRepository;
    private final TriggerPaymentUseCase triggerPaymentUseCase;

    public PaymentSchedulerJob(
            PaymentInstructionRepository paymentInstructionRepository,
            TriggerPaymentUseCase triggerPaymentUseCase) {
        this.paymentInstructionRepository = paymentInstructionRepository;
        this.triggerPaymentUseCase = triggerPaymentUseCase;
    }

    @Scheduled(fixedDelayString = "${bank-platform.payment.scheduler.poll-interval-ms:5000}")
    public void processDuePayments() {
        Instant now = Instant.now();
        List<PaymentInstruction> due = paymentInstructionRepository.findDue(now, BATCH_SIZE);
        for (PaymentInstruction instruction : due) {
            triggerPaymentUseCase.execute(instruction);
        }
    }
}
