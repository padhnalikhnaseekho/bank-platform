package com.bankplatform.reporting.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.IdempotentEventProcessor;
import com.bankplatform.reporting.application.port.AccountActivityRepository;
import com.bankplatform.reporting.domain.AccountActivityEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AccountActivityEventListenerTest {

    @Mock
    private AccountActivityRepository accountActivityRepository;

    @Mock
    private IdempotentEventProcessor idempotentEventProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AccountActivityEventListener listener;

    private record AccountCreatedPayload(String accountId, String customerId, String accountNumber, String type,
            String status, BigDecimal balance, String currency) {}

    private record MoneyMovementPayload(String transactionId, String accountId, String customerId,
            BigDecimal amount, String currency, String status, String failureReason) {}

    private record TransferPayload(String transactionId, String sourceAccountId, String targetAccountId,
            String sourceCustomerId, String targetCustomerId, BigDecimal amount, String currency,
            String failureReason) {}

    @BeforeEach
    void setUp() {
        listener = new AccountActivityEventListener(accountActivityRepository, idempotentEventProcessor,
                objectMapper);
        doAnswer(invocation -> {
            Runnable handler = invocation.getArgument(2);
            handler.run();
            return null;
        }).when(idempotentEventProcessor).process(any(), any(), any());
    }

    private String toMessage(String eventType, Object payload) {
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), eventType, 1, Instant.now(), "producer",
                "corr-1", null, "Aggregate", "agg-1", "agg-1", payload);
        return objectMapper.writeValueAsString(envelope);
    }

    @Test
    void recordsAnAccountCreatedActivity() {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String message = toMessage("account-created", new AccountCreatedPayload(accountId.toString(),
                customerId.toString(), "123456789012", "SAVINGS", "ACTIVE", BigDecimal.ZERO, "INR"));

        listener.onAccountCreated(message);

        ArgumentCaptor<AccountActivityEntry> captor = ArgumentCaptor.forClass(AccountActivityEntry.class);
        verify(accountActivityRepository).save(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("ACCOUNT_CREATED");
        assertThat(captor.getValue().accountId()).isEqualTo(accountId);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
    }

    @Test
    void recordsACompletedDeposit() {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String message = toMessage("money-deposited", new MoneyMovementPayload(UUID.randomUUID().toString(),
                accountId.toString(), customerId.toString(), new BigDecimal("50.00"), "INR", "COMPLETED", null));

        listener.onMoneyDeposited(message);

        ArgumentCaptor<AccountActivityEntry> captor = ArgumentCaptor.forClass(AccountActivityEntry.class);
        verify(accountActivityRepository).save(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("DEPOSIT");
        assertThat(captor.getValue().amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void skipsAFailedMoneyMovement() {
        String message = toMessage("money-withdrawn",
                new MoneyMovementPayload(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), new BigDecimal("50.00"), "INR", "FAILED", "Insufficient funds"));

        listener.onMoneyWithdrawn(message);

        verify(accountActivityRepository, never()).save(any());
    }

    @Test
    void recordsBothSidesOfACompletedTransfer() {
        UUID sourceCustomerId = UUID.randomUUID();
        UUID targetCustomerId = UUID.randomUUID();
        String message = toMessage("transfer-completed",
                new TransferPayload(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), sourceCustomerId.toString(), targetCustomerId.toString(),
                        new BigDecimal("40.00"), "INR", null));

        listener.onTransferCompleted(message);

        ArgumentCaptor<AccountActivityEntry> captor = ArgumentCaptor.forClass(AccountActivityEntry.class);
        verify(accountActivityRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(AccountActivityEntry::eventType)
                .containsExactlyInAnyOrder("TRANSFER_OUT", "TRANSFER_IN");
    }

    @Test
    void skipsSidesOfATransferWithNoMatchingCustomer() {
        String message = toMessage("transfer-completed",
                new TransferPayload(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), null, null, new BigDecimal("40.00"), "INR",
                        "Account not found"));

        listener.onTransferCompleted(message);

        verify(accountActivityRepository, never()).save(any());
    }
}
