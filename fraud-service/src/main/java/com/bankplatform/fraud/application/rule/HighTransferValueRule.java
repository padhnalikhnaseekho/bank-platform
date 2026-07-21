package com.bankplatform.fraud.application.rule;

import com.bankplatform.fraud.domain.FraudAlert;
import com.bankplatform.fraud.domain.FraudAlertType;
import com.bankplatform.fraud.domain.TransferWindowStats;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Flags a customer whose total outgoing transfer value exceeds {@code maxTransferAmount} within the window. */
@Component
public class HighTransferValueRule implements FraudRule {

    private final BigDecimal maxTransferAmount;

    public HighTransferValueRule(
            @Value("${bank-platform.fraud.rules.max-transfer-amount:50000}") BigDecimal maxTransferAmount) {
        this.maxTransferAmount = maxTransferAmount;
    }

    @Override
    public Optional<FraudAlert> evaluate(String customerId, TransferWindowStats stats, Instant windowStart,
            Instant windowEnd) {
        if (stats.totalAmount().compareTo(maxTransferAmount) <= 0) {
            return Optional.empty();
        }
        String message = "Customer " + customerId + " transferred " + stats.totalAmount() + " " + stats.currency()
                + " (limit " + maxTransferAmount + ") between " + windowStart + " and " + windowEnd;
        return Optional
                .of(FraudAlert.of(customerId, FraudAlertType.HIGH_TRANSFER_VALUE, stats, windowStart, windowEnd,
                        message));
    }
}
