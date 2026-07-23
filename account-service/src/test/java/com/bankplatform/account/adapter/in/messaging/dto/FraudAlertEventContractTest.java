package com.bankplatform.account.adapter.in.messaging.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankplatform.common.testfixtures.ContractFixtures;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies account-service can still read the shared fraud-alert contract fixture published by
 * fraud-service (see FraudAlertContractTest there). customerId is load-bearing here:
 * FreezeAccountsForFraudAlertUseCase freezes every ACTIVE account for that customer.
 */
class FraudAlertEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesFraudAlertContract() {
        FraudAlertEvent event = objectMapper.readValue(ContractFixtures.read("fraud-alert"), FraudAlertEvent.class);

        assertThat(event.customerId()).isEqualTo("d1a2b3c4-d5e6-4f70-8a91-0b1c2d3e4f50");
        assertThat(event.type()).isEqualTo("HIGH_TRANSFER_COUNT");
        assertThat(event.transferCount()).isEqualTo(6);
    }
}
