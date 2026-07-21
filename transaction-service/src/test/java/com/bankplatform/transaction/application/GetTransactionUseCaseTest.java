package com.bankplatform.transaction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bankplatform.common.error.NotFoundException;
import com.bankplatform.transaction.application.port.TransactionRepository;
import com.bankplatform.transaction.domain.Money;
import com.bankplatform.transaction.domain.Transaction;
import com.bankplatform.transaction.domain.TransactionId;
import com.bankplatform.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class GetTransactionUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    private GetTransactionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTransactionUseCase(transactionRepository);
    }

    @Test
    void ownerCanViewTheirOwnTransaction() {
        UUID customerId = UUID.randomUUID();
        Transaction transaction = Transaction.receive(customerId, TransactionType.DEPOSIT,
                Money.of(new BigDecimal("10"), "INR"), null, UUID.randomUUID());
        when(transactionRepository.findById(transaction.id())).thenReturn(Optional.of(transaction));

        Transaction result = useCase.getById(transaction.id(), customerId, false);

        assertThat(result).isSameAs(transaction);
    }

    @Test
    void adminCanViewAnyTransaction() {
        Transaction transaction = Transaction.receive(UUID.randomUUID(), TransactionType.DEPOSIT,
                Money.of(new BigDecimal("10"), "INR"), null, UUID.randomUUID());
        when(transactionRepository.findById(transaction.id())).thenReturn(Optional.of(transaction));

        Transaction result = useCase.getById(transaction.id(), UUID.randomUUID(), true);

        assertThat(result).isSameAs(transaction);
    }

    @Test
    void rejectsViewingSomeoneElsesTransaction() {
        Transaction transaction = Transaction.receive(UUID.randomUUID(), TransactionType.DEPOSIT,
                Money.of(new BigDecimal("10"), "INR"), null, UUID.randomUUID());
        when(transactionRepository.findById(transaction.id())).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> useCase.getById(transaction.id(), UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsUnknownTransaction() {
        TransactionId id = TransactionId.newId();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getById(id, UUID.randomUUID(), false))
                .isInstanceOf(NotFoundException.class);
    }
}
