package com.bankplatform.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.payment.application.event.PaymentCreatedPayload;
import com.bankplatform.payment.application.port.PaymentInstructionRepository;
import com.bankplatform.payment.domain.Money;
import com.bankplatform.payment.domain.PaymentInstruction;
import com.bankplatform.payment.domain.PaymentStatus;
import com.bankplatform.payment.domain.ScheduleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateRecurringPaymentUseCaseTest {

    @Mock private PaymentInstructionRepository paymentInstructionRepository;

    @Mock private EventPublisher eventPublisher;

    private CreateRecurringPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateRecurringPaymentUseCase(paymentInstructionRepository, eventPublisher);
    }

    @Test
    void createsARecurringActivePaymentWithTheGivenInterval() {
        when(paymentInstructionRepository.save(any(PaymentInstruction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        UUID customerId = UUID.randomUUID();
        UUID sourceAccountId = UUID.randomUUID();
        UUID payeeAccountId = UUID.randomUUID();
        Instant startAt = Instant.now().plusSeconds(60);
        Money amount = Money.of(new BigDecimal("1000.00"), "INR");

        PaymentInstruction result =
                useCase.execute(customerId, sourceAccountId, payeeAccountId, amount, startAt, 30);

        assertThat(result.status()).isEqualTo(PaymentStatus.ACTIVE);
        assertThat(result.schedule().type()).isEqualTo(ScheduleType.RECURRING);
        assertThat(result.schedule().intervalDays()).isEqualTo(30);
        verify(eventPublisher)
                .publish(
                        eq("payment-created"),
                        eq("PaymentInstruction"),
                        eq(result.id().toString()),
                        eq(PaymentCreatedPayload.from(result)));
    }
}
