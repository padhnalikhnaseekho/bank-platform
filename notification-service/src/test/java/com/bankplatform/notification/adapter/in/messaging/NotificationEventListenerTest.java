package com.bankplatform.notification.adapter.in.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.IdempotentEventProcessor;
import com.bankplatform.notification.application.SendNotificationUseCase;
import com.bankplatform.notification.domain.Channel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock private SendNotificationUseCase sendNotificationUseCase;

    @Mock private IdempotentEventProcessor idempotentEventProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener =
                new NotificationEventListener(
                        sendNotificationUseCase, idempotentEventProcessor, objectMapper);
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

    private record UserCreatedPayload(
            String userId, String email, String fullName, List<String> roles) {}

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

    private record NotificationRequestedPayload(
            String recipientUserId, String channel, String template, String message) {}

    private record FraudAlertPayload(
            UUID id,
            String customerId,
            String type,
            long transferCount,
            BigDecimal totalAmount,
            String currency,
            Instant windowStart,
            Instant windowEnd,
            Instant triggeredAt,
            String message) {}

    @Test
    void userCreatedSendsAWelcomeEmail() {
        UUID userId = UUID.randomUUID();
        String message =
                toMessage(
                        "user-created",
                        new UserCreatedPayload(
                                userId.toString(),
                                "alice@example.com",
                                "Alice",
                                List.of("CUSTOMER")));

        listener.onUserCreated(message);

        verify(sendNotificationUseCase)
                .send(
                        eq(userId),
                        eq(Channel.EMAIL),
                        eq("welcome"),
                        org.mockito.ArgumentMatchers.contains("Alice"));
    }

    @Test
    void moneyDepositedNotifiesTheAccountOwner() {
        UUID customerId = UUID.randomUUID();
        String message =
                toMessage(
                        "money-deposited",
                        new MoneyMovementPayload(
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                customerId.toString(),
                                new BigDecimal("50.00"),
                                "INR",
                                "COMPLETED",
                                null));

        listener.onMoneyDeposited(message);

        verify(sendNotificationUseCase)
                .send(eq(customerId), eq(Channel.EMAIL), eq("deposit-confirmation"), anyString());
    }

    @Test
    void moneyWithdrawnNotifiesTheAccountOwner() {
        UUID customerId = UUID.randomUUID();
        String message =
                toMessage(
                        "money-withdrawn",
                        new MoneyMovementPayload(
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                customerId.toString(),
                                new BigDecimal("20.00"),
                                "INR",
                                "COMPLETED",
                                null));

        listener.onMoneyWithdrawn(message);

        verify(sendNotificationUseCase)
                .send(
                        eq(customerId),
                        eq(Channel.EMAIL),
                        eq("withdrawal-confirmation"),
                        anyString());
    }

    @Test
    void moneyMovementWithNoCustomerIdIsSkipped() {
        String message =
                toMessage(
                        "money-deposited",
                        new MoneyMovementPayload(
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                null,
                                new BigDecimal("50.00"),
                                "INR",
                                "FAILED",
                                "Account not found"));

        listener.onMoneyDeposited(message);

        verify(sendNotificationUseCase, never()).send(any(), any(), any(), any());
    }

    @Test
    void transferCompletedNotifiesBothSourceAndTargetCustomers() {
        UUID sourceCustomerId = UUID.randomUUID();
        UUID targetCustomerId = UUID.randomUUID();
        String message =
                toMessage(
                        "transfer-completed",
                        new TransferPayload(
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                sourceCustomerId.toString(),
                                targetCustomerId.toString(),
                                new BigDecimal("40.00"),
                                "INR",
                                null));

        listener.onTransferCompleted(message);

        verify(sendNotificationUseCase)
                .send(
                        eq(sourceCustomerId),
                        eq(Channel.EMAIL),
                        eq("transfer-confirmation"),
                        anyString());
        verify(sendNotificationUseCase)
                .send(
                        eq(targetCustomerId),
                        eq(Channel.EMAIL),
                        eq("transfer-confirmation"),
                        anyString());
    }

    @Test
    void transferFailedOnlyNotifiesTheSourceWhenTargetAccountWasMissing() {
        UUID sourceCustomerId = UUID.randomUUID();
        String message =
                toMessage(
                        "transfer-failed",
                        new TransferPayload(
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                sourceCustomerId.toString(),
                                null,
                                new BigDecimal("40.00"),
                                "INR",
                                "Account not found"));

        listener.onTransferFailed(message);

        verify(sendNotificationUseCase)
                .send(eq(sourceCustomerId), eq(Channel.EMAIL), eq("transfer-failure"), anyString());
        verify(sendNotificationUseCase, org.mockito.Mockito.times(1))
                .send(any(), any(), any(), any());
    }

    @Test
    void fraudAlertNotifiesTheAffectedCustomer() {
        UUID customerId = UUID.randomUUID();
        String message =
                toMessage(
                        "fraud-alert",
                        new FraudAlertPayload(
                                UUID.randomUUID(),
                                customerId.toString(),
                                "HIGH_TRANSFER_COUNT",
                                6,
                                new BigDecimal("100.00"),
                                "INR",
                                Instant.now(),
                                Instant.now(),
                                Instant.now(),
                                "Customer made 6 transfers"));

        listener.onFraudAlert(message);

        verify(sendNotificationUseCase)
                .send(
                        eq(customerId),
                        eq(Channel.EMAIL),
                        eq("fraud-alert"),
                        eq("Customer made 6 transfers"));
    }

    @Test
    void notificationRequestedDispatchesToTheRequestedChannel() {
        UUID recipientId = UUID.randomUUID();
        String message =
                toMessage(
                        "notification-requested",
                        new NotificationRequestedPayload(
                                recipientId.toString(), "PUSH", "custom-template", "custom body"));

        listener.onNotificationRequested(message);

        verify(sendNotificationUseCase)
                .send(recipientId, Channel.PUSH, "custom-template", "custom body");
    }
}
