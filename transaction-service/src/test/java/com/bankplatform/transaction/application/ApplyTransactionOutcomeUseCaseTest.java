package com.bankplatform.transaction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.transaction.application.port.TransactionRepository;
import com.bankplatform.transaction.domain.Money;
import com.bankplatform.transaction.domain.Transaction;
import com.bankplatform.transaction.domain.TransactionId;
import com.bankplatform.transaction.domain.TransactionStatus;
import com.bankplatform.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplyTransactionOutcomeUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EventPublisher eventPublisher;

    private ApplyTransactionOutcomeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ApplyTransactionOutcomeUseCase(transactionRepository, eventPublisher);
    }

    private Transaction processingTransaction() {
        Transaction transaction = Transaction.receive(UUID.randomUUID(), TransactionType.DEPOSIT,
                Money.of(new BigDecimal("50.00"), "INR"), null, UUID.randomUUID());
        transaction.validate();
        transaction.markProcessing();
        return transaction;
    }

    @Test
    void marksTheTransactionCompletedAndPublishesTheStatusChange() {
        Transaction transaction = processingTransaction();
        when(transactionRepository.findById(transaction.id())).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        useCase.execute(transaction.id().toString(), true);

        assertThat(transaction.status()).isEqualTo(TransactionStatus.COMPLETED);
        verify(eventPublisher).publish(eq("transaction-status-changed"), eq("Transaction"),
                eq(transaction.id().toString()), any());
    }

    @Test
    void marksTheTransactionFailedAndPublishesTheStatusChange() {
        Transaction transaction = processingTransaction();
        when(transactionRepository.findById(transaction.id())).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        useCase.execute(transaction.id().toString(), false);

        assertThat(transaction.status()).isEqualTo(TransactionStatus.FAILED);
        verify(eventPublisher).publish(eq("transaction-status-changed"), eq("Transaction"),
                eq(transaction.id().toString()), any());
    }

    @Test
    void rejectsAnUnknownTransactionId() {
        TransactionId id = TransactionId.newId();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id.toString(), true)).isInstanceOf(IllegalStateException.class);
    }
}
