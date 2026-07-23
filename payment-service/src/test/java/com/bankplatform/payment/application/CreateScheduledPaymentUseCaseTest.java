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
class CreateScheduledPaymentUseCaseTest {

    @Mock private PaymentInstructionRepository paymentInstructionRepository;

    @Mock private EventPublisher eventPublisher;

    private CreateScheduledPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateScheduledPaymentUseCase(paymentInstructionRepository, eventPublisher);
    }

    @Test
    void createsAOneTimeActivePaymentAtTheRequestedRunTime() {
        when(paymentInstructionRepository.save(any(PaymentInstruction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        UUID customerId = UUID.randomUUID();
        UUID sourceAccountId = UUID.randomUUID();
        UUID payeeAccountId = UUID.randomUUID();
        Instant runAt = Instant.now().plusSeconds(3600);
        Money amount = Money.of(new BigDecimal("250.00"), "INR");

        PaymentInstruction result =
                useCase.execute(customerId, sourceAccountId, payeeAccountId, amount, runAt);

        assertThat(result.status()).isEqualTo(PaymentStatus.ACTIVE);
        assertThat(result.schedule().type()).isEqualTo(ScheduleType.ONE_TIME);
        assertThat(result.schedule().nextRunAt()).isEqualTo(runAt);
        verify(eventPublisher)
                .publish(
                        eq("payment-created"),
                        eq("PaymentInstruction"),
                        eq(result.id().toString()),
                        eq(PaymentCreatedPayload.from(result)));
    }
}
