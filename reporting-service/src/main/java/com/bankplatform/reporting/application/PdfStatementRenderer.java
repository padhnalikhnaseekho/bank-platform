package com.bankplatform.reporting.application;

import com.bankplatform.reporting.domain.AccountActivityEntry;
import com.bankplatform.reporting.domain.StatementJob;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

@Component
public class PdfStatementRenderer {

    private static final int LINES_PER_PAGE = 45;
    private static final float START_Y = 740f;
    private static final float LINE_HEIGHT = 16f;
    private static final float MARGIN_X = 50f;

    public byte[] render(StatementJob job, List<AccountActivityEntry> entries) {
        try (PDDocument document = new PDDocument()) {
            List<String> lines = buildLines(job, entries);
            for (int start = 0; start < Math.max(lines.size(), 1); start += LINES_PER_PAGE) {
                renderPage(document, lines.subList(start, Math.min(start + LINES_PER_PAGE, lines.size())));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render statement PDF", e);
        }
    }

    private List<String> buildLines(StatementJob job, List<AccountActivityEntry> entries) {
        List<String> lines = new java.util.ArrayList<>();
        lines.add("Account Statement");
        lines.add("Account: " + job.accountId());
        lines.add("Period: " + job.periodStart() + " to " + job.periodEnd());
        lines.add("");
        lines.add(String.format("%-20s %15s %6s %-30s", "Type", "Amount", "Curr", "Occurred At"));
        for (AccountActivityEntry entry : entries) {
            lines.add(String.format("%-20s %15s %6s %-30s", entry.eventType(), entry.amount(), entry.currency(),
                    entry.occurredAt()));
        }
        if (entries.isEmpty()) {
            lines.add("(no activity in this period)");
        }
        return lines;
    }

    private void renderPage(PDDocument document, List<String> lines) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            contentStream.newLineAtOffset(MARGIN_X, START_Y);
            for (String line : lines) {
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -LINE_HEIGHT);
            }
            contentStream.endText();
        }
    }
}
