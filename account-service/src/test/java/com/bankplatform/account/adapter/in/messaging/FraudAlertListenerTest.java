package com.bankplatform.account.adapter.in.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bankplatform.account.application.FreezeAccountsForFraudAlertUseCase;
import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.IdempotentEventProcessor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class FraudAlertListenerTest {

    @Mock
    private FreezeAccountsForFraudAlertUseCase freezeAccountsForFraudAlertUseCase;

    @Mock
    private IdempotentEventProcessor idempotentEventProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private FraudAlertListener listener;

    private record FraudAlertPayload(UUID id, String customerId, String type, long transferCount,
            BigDecimal totalAmount, String currency, Instant windowStart, Instant windowEnd, Instant triggeredAt,
            String message) {}

    @BeforeEach
    void setUp() {
        listener = new FraudAlertListener(freezeAccountsForFraudAlertUseCase, idempotentEventProcessor,
                objectMapper);
    }

    private void stubProcessorToRunHandler() {
        doAnswer(invocation -> {
            Runnable handler = invocation.getArgument(2);
            handler.run();
            return null;
        }).when(idempotentEventProcessor).process(any(), any(), any());
    }

    private String toMessage(UUID customerId) {
        FraudAlertPayload payload = new FraudAlertPayload(UUID.randomUUID(), customerId.toString(),
                "HIGH_TRANSFER_COUNT", 6, new BigDecimal("100.00"), "INR", Instant.now(), Instant.now(),
                Instant.now(), "Customer made 6 transfers");
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), "fraud-alert", 1, Instant.now(),
                "fraud-service", "corr-1", null, "Customer", customerId.toString(), customerId.toString(), payload);
        return objectMapper.writeValueAsString(envelope);
    }

    @Test
    void delegatesToTheUseCaseWithTheFlaggedCustomerId() {
        stubProcessorToRunHandler();
        UUID customerId = UUID.randomUUID();

        listener.onFraudAlert(toMessage(customerId));

        verify(freezeAccountsForFraudAlertUseCase).execute(customerId);
    }

    @Test
    void skipsAlreadyProcessedEvents() {
        UUID customerId = UUID.randomUUID();

        listener.onFraudAlert(toMessage(customerId));

        verify(freezeAccountsForFraudAlertUseCase, never()).execute(any());
    }
}
