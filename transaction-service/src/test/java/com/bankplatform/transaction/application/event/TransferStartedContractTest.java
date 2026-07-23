package com.bankplatform.transaction.application.event;

import com.bankplatform.common.testfixtures.ContractFixtures;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Verifies transaction-service's actual transfer-started payload still matches the shared
 * contract fixture consumed by account-service's TransactionCreatedEvent. A field rename here
 * without updating the fixture (and every consumer) fails this test instead of silently
 * dropping data at the consumer.
 */
class TransferStartedContractTest {

    @Test
    void matchesTransferStartedContract() {
        TransactionEventPayload payload = new TransactionEventPayload("8f14e45f-ceea-467e-adde-3fb5c8e75f92",
                "TRANSFER", "b3816b1d-8760-4f2e-9b6e-2b1f8f5d9a01", "c6f3f6f0-5b2a-4b1a-9b8b-3f7f8f5d9a02",
                new BigDecimal("250.00"), "USD");

        ContractFixtures.assertMatchesFixture("transfer-started", payload);
    }
}
