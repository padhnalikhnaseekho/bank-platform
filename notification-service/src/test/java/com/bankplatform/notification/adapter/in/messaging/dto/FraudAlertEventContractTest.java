package com.bankplatform.notification.adapter.in.messaging.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankplatform.common.testfixtures.ContractFixtures;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies notification-service can still read the shared fraud-alert contract fixture published by
 * fraud-service (see FraudAlertContractTest there).
 */
class FraudAlertEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesFraudAlertContract() {
        FraudAlertEvent event =
                objectMapper.readValue(ContractFixtures.read("fraud-alert"), FraudAlertEvent.class);

        assertThat(event.customerId()).isEqualTo("d1a2b3c4-d5e6-4f70-8a91-0b1c2d3e4f50");
        assertThat(event.message())
                .isEqualTo("Customer exceeded 5 transfers within a 10 minute window");
    }
}
