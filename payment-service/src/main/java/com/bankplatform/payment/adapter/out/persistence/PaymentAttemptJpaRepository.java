package com.bankplatform.payment.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptJpaRepository extends JpaRepository<PaymentAttemptEntity, UUID> {

    Optional<PaymentAttemptEntity> findByTransactionId(UUID transactionId);
}
