package com.bankplatform.fraud.application.rule;

import com.bankplatform.fraud.domain.FraudAlert;
import com.bankplatform.fraud.domain.TransferWindowStats;
import java.time.Instant;
import java.util.Optional;

/** A single suspicious-activity rule evaluated against one customer's sliding-window stats. */
public interface FraudRule {

    Optional<FraudAlert> evaluate(String customerId, TransferWindowStats stats, Instant windowStart,
            Instant windowEnd);
}
