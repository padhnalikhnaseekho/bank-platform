package com.bankplatform.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankplatform.common.error.ConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransactionTest {

    private final Money amount = Money.of(new java.math.BigDecimal("100"), "INR");

    @Test
    void depositRequiresTargetAccount() {
        assertThatThrownBy(
                        () ->
                                Transaction.receive(
                                        UUID.randomUUID(),
                                        TransactionType.DEPOSIT,
                                        amount,
                                        null,
                                        null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withdrawalRequiresSourceAccount() {
        assertThatThrownBy(
                        () ->
                                Transaction.receive(
                                        UUID.randomUUID(),
                                        TransactionType.WITHDRAWAL,
                                        amount,
                                        null,
                                        null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transferRequiresSourceAndTargetAccount() {
        assertThatThrownBy(
                        () ->
                                Transaction.receive(
                                        UUID.randomUUID(),
                                        TransactionType.TRANSFER,
                                        amount,
                                        UUID.randomUUID(),
                                        null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void followsReceivedValidatedProcessingCompletedLifecycle() {
        Transaction transaction =
                Transaction.receive(
                        UUID.randomUUID(),
                        TransactionType.DEPOSIT,
                        amount,
                        null,
                        UUID.randomUUID());
        assertThat(transaction.status()).isEqualTo(TransactionStatus.RECEIVED);

        transaction.validate();
        assertThat(transaction.status()).isEqualTo(TransactionStatus.VALIDATED);

        transaction.markProcessing();
        assertThat(transaction.status()).isEqualTo(TransactionStatus.PROCESSING);

        transaction.complete();
        assertThat(transaction.status()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void processingTransactionCanFailInsteadOfComplete() {
        Transaction transaction =
                Transaction.receive(
                        UUID.randomUUID(),
                        TransactionType.WITHDRAWAL,
                        amount,
                        UUID.randomUUID(),
                        null);
        transaction.validate();
        transaction.markProcessing();

        transaction.fail();

        assertThat(transaction.status()).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    void rejectsValidatingATransactionThatIsNotReceived() {
        Transaction transaction =
                Transaction.receive(
                        UUID.randomUUID(),
                        TransactionType.DEPOSIT,
                        amount,
                        null,
                        UUID.randomUUID());
        transaction.validate();

        assertThatThrownBy(transaction::validate).isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsCompletingATransactionThatIsNotProcessing() {
        Transaction transaction =
                Transaction.receive(
                        UUID.randomUUID(),
                        TransactionType.DEPOSIT,
                        amount,
                        null,
                        UUID.randomUUID());

        assertThatThrownBy(transaction::complete).isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsMarkingProcessingATransactionThatIsNotValidated() {
        Transaction transaction =
                Transaction.receive(
                        UUID.randomUUID(),
                        TransactionType.DEPOSIT,
                        amount,
                        null,
                        UUID.randomUUID());

        assertThatThrownBy(transaction::markProcessing).isInstanceOf(ConflictException.class);
    }
}
