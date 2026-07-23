package com.bankplatform.fraud.adapter.in.messaging.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankplatform.common.testfixtures.ContractFixtures;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies fraud-service can still read the shared transfer-completed contract fixture published by
 * account-service (see TransferOutcomePayloadContractTest there). sourceCustomerId in particular is
 * load-bearing here: FraudDetectionTopologyBuilder re-keys the stream by it.
 */
class TransferOutcomeEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesTransferCompletedContract() {
        TransferOutcomeEvent event =
                objectMapper.readValue(
                        ContractFixtures.read("transfer-completed"), TransferOutcomeEvent.class);

        assertThat(event.transactionId()).isEqualTo("8f14e45f-ceea-467e-adde-3fb5c8e75f92");
        assertThat(event.sourceCustomerId()).isEqualTo("d1a2b3c4-d5e6-4f70-8a91-0b1c2d3e4f50");
        assertThat(event.targetCustomerId()).isEqualTo("e2b3c4d5-e6f7-4081-9ba2-1c2d3e4f5061");
        assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(event.currency()).isEqualTo("USD");
        assertThat(event.failureReason()).isNull();
    }
}
