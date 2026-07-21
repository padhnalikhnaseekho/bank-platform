package com.bankplatform.payment.application;

import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.payment.application.event.PaymentCreatedPayload;
import com.bankplatform.payment.application.port.PaymentInstructionRepository;
import com.bankplatform.payment.domain.Money;
import com.bankplatform.payment.domain.PaymentInstruction;
import com.bankplatform.payment.domain.PaymentSchedule;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateScheduledPaymentUseCase {

    private final PaymentInstructionRepository paymentInstructionRepository;
    private final EventPublisher eventPublisher;

    public CreateScheduledPaymentUseCase(PaymentInstructionRepository paymentInstructionRepository,
            EventPublisher eventPublisher) {
        this.paymentInstructionRepository = paymentInstructionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PaymentInstruction execute(UUID customerId, UUID sourceAccountId, UUID payeeAccountId, Money amount,
            Instant runAt) {
        PaymentInstruction instruction = PaymentInstruction.create(customerId, sourceAccountId, payeeAccountId,
                amount, PaymentSchedule.oneTime(runAt));
        PaymentInstruction saved = paymentInstructionRepository.save(instruction);

        eventPublisher.publish("payment-created", "PaymentInstruction", saved.id().toString(),
                PaymentCreatedPayload.from(saved));
        return saved;
    }
}
