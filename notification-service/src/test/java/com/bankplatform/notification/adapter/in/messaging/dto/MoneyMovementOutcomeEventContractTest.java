package com.bankplatform.notification.adapter.in.messaging.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankplatform.common.testfixtures.ContractFixtures;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies notification-service can still read the shared money-deposited/money-withdrawn
 * contract fixtures published by account-service (see
 * MoneyMovementOutcomePayloadContractTest there).
 */
class MoneyMovementOutcomeEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesMoneyDepositedContract() {
        MoneyMovementOutcomeEvent event = objectMapper.readValue(ContractFixtures.read("money-deposited"),
                MoneyMovementOutcomeEvent.class);

        assertThat(event.transactionId()).isEqualTo("3d2a1b0c-9e8f-4d7c-b6a5-4938271605f4");
        assertThat(event.accountId()).isEqualTo("b3816b1d-8760-4f2e-9b6e-2b1f8f5d9a01");
        assertThat(event.customerId()).isEqualTo("d1a2b3c4-d5e6-4f70-8a91-0b1c2d3e4f50");
        assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(event.status()).isEqualTo("COMPLETED");
    }

    @Test
    void deserializesMoneyWithdrawnContract() {
        MoneyMovementOutcomeEvent event = objectMapper.readValue(ContractFixtures.read("money-withdrawn"),
                MoneyMovementOutcomeEvent.class);

        assertThat(event.status()).isEqualTo("FAILED");
        assertThat(event.failureReason()).isEqualTo("INSUFFICIENT_FUNDS");
    }
}
