package com.bankplatform.payment.application;

import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.payment.application.event.PaymentDuePayload;
import com.bankplatform.payment.application.event.TransferInitiationPayload;
import com.bankplatform.payment.application.port.PaymentAttemptRepository;
import com.bankplatform.payment.application.port.PaymentInstructionRepository;
import com.bankplatform.payment.domain.PaymentAttempt;
import com.bankplatform.payment.domain.PaymentInstruction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Triggers due payments by creating an attempt and publishing {@code transfer-started}
 * directly (the same wire shape Transaction Service uses) — Account Service owns balance
 * mutations and doesn't care which service produced the transfer request.
 */
@Component
public class PaymentSchedulerJob {

    private static final int BATCH_SIZE = 50;

    private final PaymentInstructionRepository paymentInstructionRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final EventPublisher eventPublisher;

    public PaymentSchedulerJob(PaymentInstructionRepository paymentInstructionRepository,
            PaymentAttemptRepository paymentAttemptRepository, EventPublisher eventPublisher) {
        this.paymentInstructionRepository = paymentInstructionRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${bank-platform.payment.scheduler.poll-interval-ms:5000}")
    public void processDuePayments() {
        Instant now = Instant.now();
        List<PaymentInstruction> due = paymentInstructionRepository.findDue(now, BATCH_SIZE);
        for (PaymentInstruction instruction : due) {
            triggerPayment(instruction);
        }
    }

    @Transactional
    void triggerPayment(PaymentInstruction instruction) {
        UUID transactionId = UUID.randomUUID();
        PaymentAttempt attempt = PaymentAttempt.create(instruction.id(), transactionId);
        paymentAttemptRepository.save(attempt);

        String currency = instruction.amount().currency().getCurrencyCode();

        eventPublisher.publish("payment-due", "PaymentInstruction", instruction.id().toString(),
                new PaymentDuePayload(instruction.id().toString(), attempt.id().toString(), transactionId.toString(),
                        instruction.sourceAccountId().toString(), instruction.payeeAccountId().toString(),
                        instruction.amount().amount(), currency));

        eventPublisher.publish("transfer-started", "Transaction", transactionId.toString(),
                new TransferInitiationPayload(transactionId.toString(), "TRANSFER",
                        instruction.sourceAccountId().toString(), instruction.payeeAccountId().toString(),
                        instruction.amount().amount(), currency));

        instruction.recordAttemptTriggered();
        paymentInstructionRepository.save(instruction);
    }
}
