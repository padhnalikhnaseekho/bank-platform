package com.bankplatform.reporting.adapter.in.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.IdempotentEventProcessor;
import com.bankplatform.reporting.application.RecordAccountActivityUseCase;
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
class AccountActivityEventListenerTest {

    @Mock private RecordAccountActivityUseCase recordAccountActivityUseCase;

    @Mock private IdempotentEventProcessor idempotentEventProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AccountActivityEventListener listener;

    private record AccountCreatedPayload(
            String accountId,
            String customerId,
            String accountNumber,
            String type,
            String status,
            BigDecimal balance,
            String currency) {}

    private record MoneyMovementPayload(
            String transactionId,
            String accountId,
            String customerId,
            BigDecimal amount,
            String currency,
            String status,
            String failureReason) {}

    private record TransferPayload(
            String transactionId,
            String sourceAccountId,
            String targetAccountId,
            String sourceCustomerId,
            String targetCustomerId,
            BigDecimal amount,
            String currency,
            String failureReason) {}

    @BeforeEach
    void setUp() {
        listener =
                new AccountActivityEventListener(
                        recordAccountActivityUseCase, idempotentEventProcessor, objectMapper);
    }

    private void stubProcessorToRunHandler() {
        doAnswer(
                        invocation -> {
                            Runnable handler = invocation.getArgument(2);
                            handler.run();
                            return null;
                        })
                .when(idempotentEventProcessor)
                .process(any(), any(), any());
    }

    private String toMessage(String eventType, Object payload) {
        EventEnvelope envelope =
                new EventEnvelope(
                        UUID.randomUUID(),
                        eventType,
                        1,
                        Instant.now(),
                        "producer",
                        "corr-1",
                        null,
                        "Aggregate",
                        "agg-1",
                        "agg-1",
                        payload);
        return objectMapper.writeValueAsString(envelope);
    }

    @Test
    void delegatesAccountCreatedToTheUseCase() {
        stubProcessorToRunHandler();
        String message =
                toMessage(
                        "account-created",
                        new AccountCreatedPayload(
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                "123456789012",
                                "SAVINGS",
                                "ACTIVE",
                                BigDecimal.ZERO,
                                "INR"));

        listener.onAccountCreated(message);

        verify(recordAccountActivityUseCase).recordAccountCreated(any(), any());
    }

    @Test
    void delegatesMoneyDepositedToTheUseCaseWithTheDepositEventType() {
        stubProcessorToRunHandler();
        String message =
                toMessage(
                        "money-deposited",
                        new MoneyMovementPayload(
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                new BigDecimal("50.00"),
                                "INR",
                                "COMPLETED",
                                null));

        listener.onMoneyDeposited(message);

        verify(recordAccountActivityUseCase).recordMoneyMovement(any(), eq("DEPOSIT"), any());
    }

    @Test
    void delegatesMoneyWithdrawnToTheUseCaseWithTheWithdrawalEventType() {
        stubProcessorToRunHandler();
        String message =
                toMessage(
                        "money-withdrawn",
                        new MoneyMovementPayload(
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                new BigDecimal("50.00"),
                                "INR",
                                "COMPLETED",
                                null));

        listener.onMoneyWithdrawn(message);

        verify(recordAccountActivityUseCase).recordMoneyMovement(any(), eq("WITHDRAWAL"), any());
    }

    @Test
    void delegatesTransferCompletedToTheUseCase() {
        stubProcessorToRunHandler();
        String message =
                toMessage(
                        "transfer-completed",
                        new TransferPayload(
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                new BigDecimal("40.00"),
                                "INR",
                                null));

        listener.onTransferCompleted(message);

        verify(recordAccountActivityUseCase).recordTransfer(any(), any());
    }

    @Test
    void skipsAlreadyProcessedEvents() {
        String message =
                toMessage(
                        "account-created",
                        new AccountCreatedPayload(
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                "123456789012",
                                "SAVINGS",
                                "ACTIVE",
                                BigDecimal.ZERO,
                                "INR"));

        listener.onAccountCreated(message);

        verify(recordAccountActivityUseCase, never()).recordAccountCreated(any(), any());
    }
}
