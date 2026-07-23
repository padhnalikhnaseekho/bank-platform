package com.bankplatform.transaction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.common.error.ValidationException;
import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.transaction.application.event.TransactionEventPayload;
import com.bankplatform.transaction.application.port.TransactionRepository;
import com.bankplatform.transaction.domain.Money;
import com.bankplatform.transaction.domain.Transaction;
import com.bankplatform.transaction.domain.TransactionStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateTransferUseCaseTest {

    @Mock private TransactionRepository transactionRepository;

    @Mock private EventPublisher eventPublisher;

    private CreateTransferUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateTransferUseCase(transactionRepository, eventPublisher);
    }

    @Test
    void createsAndPersistsAProcessingTransfer() {
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        UUID customerId = UUID.randomUUID();
        UUID sourceAccountId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        Money amount = Money.of(new BigDecimal("75.00"), "INR");

        Transaction result = useCase.execute(customerId, sourceAccountId, targetAccountId, amount);

        assertThat(result.status()).isEqualTo(TransactionStatus.PROCESSING);
        assertThat(result.sourceAccountId()).isEqualTo(sourceAccountId);
        assertThat(result.targetAccountId()).isEqualTo(targetAccountId);
        verify(eventPublisher)
                .publish(
                        "transfer-started",
                        "Transaction",
                        result.id().toString(),
                        new TransactionEventPayload(
                                result.id().toString(),
                                "TRANSFER",
                                sourceAccountId.toString(),
                                targetAccountId.toString(),
                                amount.amount(),
                                "INR"));
    }

    @Test
    void rejectsTransferToTheSameAccount() {
        UUID accountId = UUID.randomUUID();
        Money amount = Money.of(new BigDecimal("10.00"), "INR");

        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), accountId, accountId, amount))
                .isInstanceOf(ValidationException.class);
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any(), any(), any());
    }
}
