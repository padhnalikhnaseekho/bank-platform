package com.bankplatform.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bankplatform.reporting.adapter.in.messaging.dto.AccountCreatedEvent;
import com.bankplatform.reporting.adapter.in.messaging.dto.MoneyMovementOutcomeEvent;
import com.bankplatform.reporting.adapter.in.messaging.dto.TransferOutcomeEvent;
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

@ExtendWith(MockitoExtension.class)
class RecordAccountActivityUseCaseTest {

    @Mock
    private AccountActivityRepository accountActivityRepository;

    private RecordAccountActivityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RecordAccountActivityUseCase(accountActivityRepository);
    }

    @Test
    void recordsAnAccountCreatedActivity() {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        useCase.recordAccountCreated(new AccountCreatedEvent(accountId.toString(), customerId.toString(),
                "123456789012", "SAVINGS", "ACTIVE", BigDecimal.ZERO, "INR"), Instant.now());

        ArgumentCaptor<AccountActivityEntry> captor = ArgumentCaptor.forClass(AccountActivityEntry.class);
        verify(accountActivityRepository).save(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("ACCOUNT_CREATED");
        assertThat(captor.getValue().accountId()).isEqualTo(accountId);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
    }

    @Test
    void recordsACompletedMoneyMovement() {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        useCase.recordMoneyMovement(new MoneyMovementOutcomeEvent(UUID.randomUUID().toString(), accountId.toString(),
                customerId.toString(), new BigDecimal("50.00"), "INR", "COMPLETED", null), "DEPOSIT", Instant.now());

        ArgumentCaptor<AccountActivityEntry> captor = ArgumentCaptor.forClass(AccountActivityEntry.class);
        verify(accountActivityRepository).save(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("DEPOSIT");
        assertThat(captor.getValue().amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void skipsAFailedMoneyMovement() {
        useCase.recordMoneyMovement(new MoneyMovementOutcomeEvent(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), new BigDecimal("50.00"), "INR", "FAILED",
                "Insufficient funds"), "WITHDRAWAL", Instant.now());

        verify(accountActivityRepository, never()).save(any());
    }

    @Test
    void skipsAMoneyMovementWithNoCustomerId() {
        useCase.recordMoneyMovement(new MoneyMovementOutcomeEvent(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, new BigDecimal("50.00"), "INR", "COMPLETED", null), "DEPOSIT",
                Instant.now());

        verify(accountActivityRepository, never()).save(any());
    }

    @Test
    void recordsBothSidesOfACompletedTransfer() {
        UUID sourceCustomerId = UUID.randomUUID();
        UUID targetCustomerId = UUID.randomUUID();

        useCase.recordTransfer(new TransferOutcomeEvent(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), sourceCustomerId.toString(), targetCustomerId.toString(),
                new BigDecimal("40.00"), "INR", null), Instant.now());

        ArgumentCaptor<AccountActivityEntry> captor = ArgumentCaptor.forClass(AccountActivityEntry.class);
        verify(accountActivityRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(AccountActivityEntry::eventType)
                .containsExactlyInAnyOrder("TRANSFER_OUT", "TRANSFER_IN");
    }

    @Test
    void skipsSidesOfATransferWithNoMatchingCustomer() {
        useCase.recordTransfer(new TransferOutcomeEvent(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, null, new BigDecimal("40.00"), "INR", "Account not found"),
                Instant.now());

        verify(accountActivityRepository, never()).save(any());
    }
}
