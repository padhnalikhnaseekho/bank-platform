package com.bankplatform.fraud.domain;

import com.bankplatform.common.testfixtures.ContractFixtures;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Verifies fraud-service's actual fraud-alert payload still matches the shared contract
 * fixture consumed by account-service and notification-service's own copies of this DTO.
 */
class FraudAlertContractTest {

    @Test
    void matchesFraudAlertContract() {
        FraudAlert alert = new FraudAlert(UUID.fromString("a1b2c3d4-e5f6-4708-8901-2a3b4c5d6e7f"),
                "d1a2b3c4-d5e6-4f70-8a91-0b1c2d3e4f50", FraudAlertType.HIGH_TRANSFER_COUNT, 6,
                new BigDecimal("62000.00"), "USD", Instant.parse("2026-07-21T15:50:00Z"),
                Instant.parse("2026-07-21T16:00:00Z"), Instant.parse("2026-07-21T16:00:00Z"),
                "Customer exceeded 5 transfers within a 10 minute window");

        ContractFixtures.assertMatchesFixture("fraud-alert", alert);
    }
}
