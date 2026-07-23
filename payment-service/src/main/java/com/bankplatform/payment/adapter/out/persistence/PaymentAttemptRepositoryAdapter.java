package com.bankplatform.payment.adapter.out.persistence;

import com.bankplatform.payment.application.port.PaymentAttemptRepository;
import com.bankplatform.payment.domain.PaymentAttempt;
import com.bankplatform.payment.domain.PaymentAttemptId;
import com.bankplatform.payment.domain.PaymentAttemptStatus;
import com.bankplatform.payment.domain.PaymentId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PaymentAttemptRepositoryAdapter implements PaymentAttemptRepository {

    private final PaymentAttemptJpaRepository jpaRepository;

    public PaymentAttemptRepositoryAdapter(PaymentAttemptJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PaymentAttempt save(PaymentAttempt attempt) {
        PaymentAttemptEntity saved =
                jpaRepository.save(
                        new PaymentAttemptEntity(
                                attempt.id().value(),
                                attempt.paymentInstructionId().value(),
                                attempt.transactionId(),
                                attempt.status().name(),
                                attempt.failureReason(),
                                attempt.attemptedAt()));
        return toDomain(saved);
    }

    @Override
    public Optional<PaymentAttempt> findByTransactionId(UUID transactionId) {
        return jpaRepository.findByTransactionId(transactionId).map(this::toDomain);
    }

    private PaymentAttempt toDomain(PaymentAttemptEntity entity) {
        return new PaymentAttempt(
                PaymentAttemptId.of(entity.getId()),
                PaymentId.of(entity.getPaymentInstructionId()),
                entity.getTransactionId(),
                PaymentAttemptStatus.valueOf(entity.getStatus()),
                entity.getFailureReason(),
                entity.getAttemptedAt());
    }
}
