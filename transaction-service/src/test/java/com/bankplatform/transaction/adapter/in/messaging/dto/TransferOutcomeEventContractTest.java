package com.bankplatform.transaction.adapter.in.messaging.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankplatform.common.testfixtures.ContractFixtures;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies transaction-service can still read the shared transfer-completed/transfer-failed
 * contract fixtures published by account-service (see TransferOutcomePayloadContractTest there).
 * This DTO deliberately only declares a subset of the contract's fields (no
 * sourceCustomerId/targetCustomerId, unlike every other consumer) — this test locks in that this is
 * a subset by choice, not by an accidental typo.
 */
class TransferOutcomeEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesTransferCompletedContract() {
        TransferOutcomeEvent event =
                objectMapper.readValue(
                        ContractFixtures.read("transfer-completed"), TransferOutcomeEvent.class);

        assertThat(event.transactionId()).isEqualTo("8f14e45f-ceea-467e-adde-3fb5c8e75f92");
        assertThat(event.sourceAccountId()).isEqualTo("b3816b1d-8760-4f2e-9b6e-2b1f8f5d9a01");
        assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(event.failureReason()).isNull();
    }

    @Test
    void deserializesTransferFailedContract() {
        TransferOutcomeEvent event =
                objectMapper.readValue(
                        ContractFixtures.read("transfer-failed"), TransferOutcomeEvent.class);

        assertThat(event.failureReason()).isEqualTo("INSUFFICIENT_FUNDS");
    }
}
