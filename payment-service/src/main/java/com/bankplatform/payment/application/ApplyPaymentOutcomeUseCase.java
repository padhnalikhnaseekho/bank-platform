package com.bankplatform.payment.application;

import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.payment.application.event.PaymentOutcomePayload;
import com.bankplatform.payment.application.port.PaymentAttemptRepository;
import com.bankplatform.payment.domain.PaymentAttempt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kept as its own bean (rather than inlined in PaymentOutcomeListener) so the transactional
 * boundary is enforced through a real Spring proxy — calling an {@code @Transactional} method on
 * {@code this} from within the same class silently skips the transaction, since self-invocation
 * never goes through the proxy, in CGLIB just as much as JDK proxies.
 */
@Service
public class ApplyPaymentOutcomeUseCase {

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final EventPublisher eventPublisher;

    public ApplyPaymentOutcomeUseCase(
            PaymentAttemptRepository paymentAttemptRepository, EventPublisher eventPublisher) {
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(UUID transactionId, boolean success, String failureReason) {
        Optional<PaymentAttempt> maybeAttempt =
                paymentAttemptRepository.findByTransactionId(transactionId);
        if (maybeAttempt.isEmpty()) {
            return;
        }
        PaymentAttempt attempt = maybeAttempt.get();
        if (success) {
            attempt.markSucceeded();
        } else {
            attempt.markFailed(failureReason);
        }
        paymentAttemptRepository.save(attempt);

        String eventType = success ? "payment-success" : "payment-failed";
        eventPublisher.publish(
                eventType,
                "PaymentAttempt",
                attempt.id().toString(),
                new PaymentOutcomePayload(
                        attempt.paymentInstructionId().toString(),
                        attempt.id().toString(),
                        transactionId.toString(),
                        attempt.status().name(),
                        attempt.failureReason()));
    }
}
