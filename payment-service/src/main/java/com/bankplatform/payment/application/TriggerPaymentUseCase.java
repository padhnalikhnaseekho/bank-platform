package com.bankplatform.payment.application;

import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.payment.application.event.PaymentDuePayload;
import com.bankplatform.payment.application.event.TransferInitiationPayload;
import com.bankplatform.payment.application.port.PaymentAttemptRepository;
import com.bankplatform.payment.application.port.PaymentInstructionRepository;
import com.bankplatform.payment.domain.PaymentAttempt;
import com.bankplatform.payment.domain.PaymentInstruction;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kept as its own bean (rather than inlined in PaymentSchedulerJob) so the transactional
 * boundary is enforced through a real Spring proxy — calling an {@code @Transactional}
 * method on {@code this} from within the same class silently skips the transaction, since
 * self-invocation never goes through the proxy, in CGLIB just as much as JDK proxies.
 *
 * <p>Triggers a due payment by creating an attempt and publishing {@code transfer-started}
 * directly (the same wire shape Transaction Service uses) — Account Service owns balance
 * mutations and doesn't care which service produced the transfer request.
 */
@Service
public class TriggerPaymentUseCase {

    private final PaymentInstructionRepository paymentInstructionRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final EventPublisher eventPublisher;

    public TriggerPaymentUseCase(PaymentInstructionRepository paymentInstructionRepository,
            PaymentAttemptRepository paymentAttemptRepository, EventPublisher eventPublisher) {
        this.paymentInstructionRepository = paymentInstructionRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(PaymentInstruction instruction) {
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
