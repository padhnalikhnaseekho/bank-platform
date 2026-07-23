package com.bankplatform.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankplatform.reporting.domain.AccountActivityEntry;
import com.bankplatform.reporting.domain.StatementJob;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

class PdfStatementRendererTest {

    private final PdfStatementRenderer renderer = new PdfStatementRenderer();

    @Test
    void rendersAValidPdfDocumentWithNoEntries() {
        StatementJob job =
                StatementJob.request(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.now().minusSeconds(60),
                        Instant.now());

        byte[] pdf = renderer.render(job, List.of());

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(0);
    }

    @Test
    void rendersAValidPdfDocumentWithEntries() {
        StatementJob job =
                StatementJob.request(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.now().minusSeconds(60),
                        Instant.now());
        AccountActivityEntry entry =
                AccountActivityEntry.create(
                        job.customerId(),
                        job.accountId(),
                        "DEPOSIT",
                        new BigDecimal("100.00"),
                        "INR",
                        Instant.now());

        byte[] pdf = renderer.render(job, List.of(entry));

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void spansMultiplePagesWhenThereAreManyEntries() {
        StatementJob job =
                StatementJob.request(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.now().minusSeconds(60),
                        Instant.now());
        List<AccountActivityEntry> entries =
                java.util.stream.IntStream.range(0, 100)
                        .mapToObj(
                                i ->
                                        AccountActivityEntry.create(
                                                job.customerId(),
                                                job.accountId(),
                                                "DEPOSIT",
                                                new BigDecimal("1.00"),
                                                "INR",
                                                Instant.now()))
                        .toList();

        byte[] pdf = renderer.render(job, entries);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
