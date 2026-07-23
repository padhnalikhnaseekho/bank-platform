package com.bankplatform.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bankplatform.common.error.NotFoundException;
import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.payment.application.port.PaymentInstructionRepository;
import com.bankplatform.payment.domain.Money;
import com.bankplatform.payment.domain.PaymentId;
import com.bankplatform.payment.domain.PaymentInstruction;
import com.bankplatform.payment.domain.PaymentSchedule;
import com.bankplatform.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CancelPaymentUseCaseTest {

    @Mock private PaymentInstructionRepository paymentInstructionRepository;

    @Mock private EventPublisher eventPublisher;

    private CancelPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CancelPaymentUseCase(paymentInstructionRepository, eventPublisher);
    }

    private PaymentInstruction anActiveInstruction(UUID customerId) {
        return PaymentInstruction.create(
                customerId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money.of(new BigDecimal("50"), "INR"),
                PaymentSchedule.oneTime(Instant.now().plusSeconds(3600)));
    }

    @Test
    void ownerCanCancelTheirOwnPayment() {
        UUID customerId = UUID.randomUUID();
        PaymentInstruction instruction = anActiveInstruction(customerId);
        when(paymentInstructionRepository.findById(instruction.id()))
                .thenReturn(Optional.of(instruction));
        when(paymentInstructionRepository.save(any(PaymentInstruction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentInstruction result = useCase.execute(instruction.id(), customerId, false);

        assertThat(result.status()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void adminCanCancelAnyPayment() {
        PaymentInstruction instruction = anActiveInstruction(UUID.randomUUID());
        when(paymentInstructionRepository.findById(instruction.id()))
                .thenReturn(Optional.of(instruction));
        when(paymentInstructionRepository.save(any(PaymentInstruction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentInstruction result = useCase.execute(instruction.id(), UUID.randomUUID(), true);

        assertThat(result.status()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void rejectsCancellingSomeoneElsesPayment() {
        PaymentInstruction instruction = anActiveInstruction(UUID.randomUUID());
        when(paymentInstructionRepository.findById(instruction.id()))
                .thenReturn(Optional.of(instruction));

        assertThatThrownBy(() -> useCase.execute(instruction.id(), UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsCancellingAnUnknownPayment() {
        PaymentId id = PaymentId.newId();
        when(paymentInstructionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id, UUID.randomUUID(), false))
                .isInstanceOf(NotFoundException.class);
    }
}
