package com.bankplatform.reporting.application;

import com.bankplatform.reporting.application.port.AccountActivityRepository;
import com.bankplatform.reporting.application.port.ReportStorage;
import com.bankplatform.reporting.application.port.StatementJobRepository;
import com.bankplatform.reporting.domain.AccountActivityEntry;
import com.bankplatform.reporting.domain.StatementJob;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerateStatementUseCase {

    private static final Logger log = LoggerFactory.getLogger(GenerateStatementUseCase.class);

    private final AccountActivityRepository accountActivityRepository;
    private final StatementJobRepository statementJobRepository;
    private final ReportStorage reportStorage;
    private final CsvStatementRenderer csvStatementRenderer;
    private final PdfStatementRenderer pdfStatementRenderer;

    public GenerateStatementUseCase(
            AccountActivityRepository accountActivityRepository,
            StatementJobRepository statementJobRepository,
            ReportStorage reportStorage,
            CsvStatementRenderer csvStatementRenderer,
            PdfStatementRenderer pdfStatementRenderer) {
        this.accountActivityRepository = accountActivityRepository;
        this.statementJobRepository = statementJobRepository;
        this.reportStorage = reportStorage;
        this.csvStatementRenderer = csvStatementRenderer;
        this.pdfStatementRenderer = pdfStatementRenderer;
    }

    @Transactional
    public StatementJob execute(
            UUID customerId, UUID accountId, Instant periodStart, Instant periodEnd) {
        StatementJob job = StatementJob.request(customerId, accountId, periodStart, periodEnd);
        StatementJob saved = statementJobRepository.save(job);

        try {
            List<AccountActivityEntry> entries =
                    accountActivityRepository.findByAccountAndPeriod(
                            accountId, periodStart, periodEnd);
            byte[] csv = csvStatementRenderer.render(entries);
            byte[] pdf = pdfStatementRenderer.render(saved, entries);

            String keyPrefix = "statements/" + saved.id() + "/statement";
            String csvUrl = reportStorage.upload(keyPrefix + ".csv", csv, "text/csv");
            String pdfUrl = reportStorage.upload(keyPrefix + ".pdf", pdf, "application/pdf");

            saved.complete(csvUrl, pdfUrl);
        } catch (RuntimeException e) {
            log.warn("Failed to generate statement {}: {}", saved.id(), e.getMessage());
            saved.fail();
        }
        return statementJobRepository.save(saved);
    }
}
