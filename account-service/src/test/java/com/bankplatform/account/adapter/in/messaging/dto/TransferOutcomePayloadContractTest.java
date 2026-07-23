package com.bankplatform.account.adapter.in.messaging.dto;

import com.bankplatform.common.testfixtures.ContractFixtures;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Verifies account-service's actual transfer-completed/transfer-failed payload still matches the
 * shared contract fixtures consumed by fraud-service, notification-service, payment-service,
 * reporting-service, and transaction-service's own copies of this DTO.
 */
class TransferOutcomePayloadContractTest {

    @Test
    void matchesTransferCompletedContract() {
        TransferOutcomePayload payload =
                new TransferOutcomePayload(
                        "8f14e45f-ceea-467e-adde-3fb5c8e75f92",
                        "b3816b1d-8760-4f2e-9b6e-2b1f8f5d9a01",
                        "c6f3f6f0-5b2a-4b1a-9b8b-3f7f8f5d9a02",
                        "d1a2b3c4-d5e6-4f70-8a91-0b1c2d3e4f50",
                        "e2b3c4d5-e6f7-4081-9ba2-1c2d3e4f5061",
                        new BigDecimal("250.00"),
                        "USD",
                        null);

        ContractFixtures.assertMatchesFixture("transfer-completed", payload);
    }

    @Test
    void matchesTransferFailedContract() {
        TransferOutcomePayload payload =
                new TransferOutcomePayload(
                        "8f14e45f-ceea-467e-adde-3fb5c8e75f92",
                        "b3816b1d-8760-4f2e-9b6e-2b1f8f5d9a01",
                        "c6f3f6f0-5b2a-4b1a-9b8b-3f7f8f5d9a02",
                        "d1a2b3c4-d5e6-4f70-8a91-0b1c2d3e4f50",
                        "e2b3c4d5-e6f7-4081-9ba2-1c2d3e4f5061",
                        new BigDecimal("250.00"),
                        "USD",
                        "INSUFFICIENT_FUNDS");

        ContractFixtures.assertMatchesFixture("transfer-failed", payload);
    }
}
