package com.bankplatform.fraud.application.rule;

import com.bankplatform.fraud.domain.FraudAlert;
import com.bankplatform.fraud.domain.FraudAlertType;
import com.bankplatform.fraud.domain.TransferWindowStats;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Flags a customer who initiates more than {@code maxTransferCount} transfers within the window. */
@Component
public class HighTransferCountRule implements FraudRule {

    private final long maxTransferCount;

    public HighTransferCountRule(@Value("${bank-platform.fraud.rules.max-transfer-count:5}") long maxTransferCount) {
        this.maxTransferCount = maxTransferCount;
    }

    @Override
    public Optional<FraudAlert> evaluate(String customerId, TransferWindowStats stats, Instant windowStart,
            Instant windowEnd) {
        if (stats.count() <= maxTransferCount) {
            return Optional.empty();
        }
        String message = "Customer " + customerId + " made " + stats.count() + " transfers (limit "
                + maxTransferCount + ") between " + windowStart + " and " + windowEnd;
        return Optional
                .of(FraudAlert.of(customerId, FraudAlertType.HIGH_TRANSFER_COUNT, stats, windowStart, windowEnd,
                        message));
    }
}
