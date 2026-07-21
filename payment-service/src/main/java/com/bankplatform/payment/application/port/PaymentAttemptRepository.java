package com.bankplatform.payment.application.port;

import com.bankplatform.payment.domain.PaymentAttempt;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository {

    PaymentAttempt save(PaymentAttempt attempt);

    Optional<PaymentAttempt> findByTransactionId(UUID transactionId);
}
