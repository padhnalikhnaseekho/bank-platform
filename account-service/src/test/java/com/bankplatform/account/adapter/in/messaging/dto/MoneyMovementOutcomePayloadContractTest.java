package com.bankplatform.account.adapter.in.messaging.dto;

import com.bankplatform.common.testfixtures.ContractFixtures;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Verifies account-service's actual money-deposited/money-withdrawn payload still matches the
 * shared contract fixtures consumed by notification-service, reporting-service, and
 * transaction-service's own copies of this DTO.
 */
class MoneyMovementOutcomePayloadContractTest {

    @Test
    void matchesMoneyDepositedContract() {
        MoneyMovementOutcomePayload payload = new MoneyMovementOutcomePayload("3d2a1b0c-9e8f-4d7c-b6a5-4938271605f4",
                "b3816b1d-8760-4f2e-9b6e-2b1f8f5d9a01", "d1a2b3c4-d5e6-4f70-8a91-0b1c2d3e4f50",
                new BigDecimal("100.00"), "USD", "COMPLETED", null);

        ContractFixtures.assertMatchesFixture("money-deposited", payload);
    }

    @Test
    void matchesMoneyWithdrawnContract() {
        MoneyMovementOutcomePayload payload = new MoneyMovementOutcomePayload("3d2a1b0c-9e8f-4d7c-b6a5-4938271605f4",
                "b3816b1d-8760-4f2e-9b6e-2b1f8f5d9a01", "d1a2b3c4-d5e6-4f70-8a91-0b1c2d3e4f50",
                new BigDecimal("100.00"), "USD", "FAILED", "INSUFFICIENT_FUNDS");

        ContractFixtures.assertMatchesFixture("money-withdrawn", payload);
    }
}
