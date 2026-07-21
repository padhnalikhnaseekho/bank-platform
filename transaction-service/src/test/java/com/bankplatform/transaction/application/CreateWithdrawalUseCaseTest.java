package com.bankplatform.transaction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class CreateWithdrawalUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EventPublisher eventPublisher;

    private CreateWithdrawalUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateWithdrawalUseCase(transactionRepository, eventPublisher);
    }

    @Test
    void createsAndPersistsAProcessingWithdrawal() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Money amount = Money.of(new BigDecimal("25.00"), "INR");

        Transaction result = useCase.execute(customerId, accountId, amount);

        assertThat(result.status()).isEqualTo(TransactionStatus.PROCESSING);
        assertThat(result.sourceAccountId()).isEqualTo(accountId);
        assertThat(result.targetAccountId()).isNull();
        verify(eventPublisher).publish("transaction-created", "Transaction", result.id().toString(),
                new TransactionEventPayload(result.id().toString(), "WITHDRAWAL", accountId.toString(), null,
                        amount.amount(), "INR"));
    }
}
