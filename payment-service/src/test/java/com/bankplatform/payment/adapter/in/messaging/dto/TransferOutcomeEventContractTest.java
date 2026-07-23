package com.bankplatform.payment.adapter.in.messaging.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankplatform.common.testfixtures.ContractFixtures;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies payment-service can still read the shared transfer-completed/transfer-failed
 * contract fixtures published by account-service (see
 * TransferOutcomePayloadContractTest there).
 */
class TransferOutcomeEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesTransferCompletedContract() {
        TransferOutcomeEvent event = objectMapper.readValue(ContractFixtures.read("transfer-completed"),
                TransferOutcomeEvent.class);

        assertThat(event.transactionId()).isEqualTo("8f14e45f-ceea-467e-adde-3fb5c8e75f92");
        assertThat(event.failureReason()).isNull();
    }

    @Test
    void deserializesTransferFailedContract() {
        TransferOutcomeEvent event = objectMapper.readValue(ContractFixtures.read("transfer-failed"),
                TransferOutcomeEvent.class);

        assertThat(event.failureReason()).isEqualTo("INSUFFICIENT_FUNDS");
    }
}
