package com.bankplatform.payment.application;

import com.bankplatform.common.error.NotFoundException;
import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.payment.application.event.PaymentCancelledPayload;
import com.bankplatform.payment.application.port.PaymentInstructionRepository;
import com.bankplatform.payment.domain.PaymentId;
import com.bankplatform.payment.domain.PaymentInstruction;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelPaymentUseCase {

    private final PaymentInstructionRepository paymentInstructionRepository;
    private final EventPublisher eventPublisher;

    public CancelPaymentUseCase(
            PaymentInstructionRepository paymentInstructionRepository,
            EventPublisher eventPublisher) {
        this.paymentInstructionRepository = paymentInstructionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PaymentInstruction execute(PaymentId id, UUID requesterId, boolean isAdmin) {
        PaymentInstruction instruction =
                paymentInstructionRepository
                        .findById(id)
                        .orElseThrow(() -> new NotFoundException("Payment not found"));
        if (!isAdmin && !instruction.customerId().equals(requesterId)) {
            throw new AccessDeniedException("Not authorized to cancel this payment");
        }
        instruction.cancel();
        PaymentInstruction saved = paymentInstructionRepository.save(instruction);

        eventPublisher.publish(
                "payment-cancelled",
                "PaymentInstruction",
                saved.id().toString(),
                new PaymentCancelledPayload(saved.id().toString(), saved.customerId().toString()));
        return saved;
    }
}
