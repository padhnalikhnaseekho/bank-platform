package com.bankplatform.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankplatform.reporting.domain.AccountActivityEntry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CsvStatementRendererTest {

    private final CsvStatementRenderer renderer = new CsvStatementRenderer();

    @Test
    void rendersAHeaderRowEvenWithNoEntries() {
        String csv = new String(renderer.render(List.of()), StandardCharsets.UTF_8);

        assertThat(csv).isEqualTo("eventType,amount,currency,occurredAt\n");
    }

    @Test
    void rendersOneRowPerEntry() {
        AccountActivityEntry entry =
                AccountActivityEntry.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "DEPOSIT",
                        new BigDecimal("100.00"),
                        "INR",
                        Instant.parse("2026-01-01T00:00:00Z"));

        String csv = new String(renderer.render(List.of(entry)), StandardCharsets.UTF_8);

        assertThat(csv)
                .isEqualTo(
                        "eventType,amount,currency,occurredAt\nDEPOSIT,100.00,INR,2026-01-01T00:00:00Z\n");
    }
}
