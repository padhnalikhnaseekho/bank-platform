package com.bankplatform.account.adapter.in.messaging.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankplatform.common.testfixtures.ContractFixtures;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies account-service can still read the shared transfer-started contract fixture published by
 * transaction-service (see TransferStartedContractTest there). A field this class declares that no
 * longer exists (or was renamed) in the fixture deserializes to null here instead of failing
 * silently at runtime.
 */
class TransactionCreatedEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesTransferStartedContract() {
        TransactionCreatedEvent event =
                objectMapper.readValue(
                        ContractFixtures.read("transfer-started"), TransactionCreatedEvent.class);

        assertThat(event.transactionId()).isEqualTo("8f14e45f-ceea-467e-adde-3fb5c8e75f92");
        assertThat(event.type()).isEqualTo("TRANSFER");
        assertThat(event.sourceAccountId()).isEqualTo("b3816b1d-8760-4f2e-9b6e-2b1f8f5d9a01");
        assertThat(event.targetAccountId()).isEqualTo("c6f3f6f0-5b2a-4b1a-9b8b-3f7f8f5d9a02");
        assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(event.currency()).isEqualTo("USD");
    }
}
