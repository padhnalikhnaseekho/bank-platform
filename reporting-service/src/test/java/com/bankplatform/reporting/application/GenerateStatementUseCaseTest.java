package com.bankplatform.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.reporting.application.port.AccountActivityRepository;
import com.bankplatform.reporting.application.port.ReportStorage;
import com.bankplatform.reporting.application.port.StatementJobRepository;
import com.bankplatform.reporting.domain.StatementJob;
import com.bankplatform.reporting.domain.StatementStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerateStatementUseCaseTest {

    @Mock
    private AccountActivityRepository accountActivityRepository;

    @Mock
    private StatementJobRepository statementJobRepository;

    @Mock
    private ReportStorage reportStorage;

    @Mock
    private CsvStatementRenderer csvStatementRenderer;

    @Mock
    private PdfStatementRenderer pdfStatementRenderer;

    private GenerateStatementUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GenerateStatementUseCase(accountActivityRepository, statementJobRepository, reportStorage,
                csvStatementRenderer, pdfStatementRenderer);
        when(statementJobRepository.save(any(StatementJob.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void completesTheJobAndUploadsBothRenderedFiles() {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        when(accountActivityRepository.findByAccountAndPeriod(accountId, from, to)).thenReturn(List.of());
        when(csvStatementRenderer.render(List.of())).thenReturn("csv".getBytes());
        when(pdfStatementRenderer.render(any(StatementJob.class), any())).thenReturn("pdf".getBytes());
        when(reportStorage.upload(anyString(), any(), anyString())).thenReturn("s3://bucket/key");

        StatementJob result = useCase.execute(customerId, accountId, from, to);

        assertThat(result.status()).isEqualTo(StatementStatus.COMPLETED);
        assertThat(result.csvFileUrl()).isEqualTo("s3://bucket/key");
        assertThat(result.pdfFileUrl()).isEqualTo("s3://bucket/key");
        verify(reportStorage).upload(org.mockito.ArgumentMatchers.contains(".csv"), any(), org.mockito.ArgumentMatchers.eq("text/csv"));
        verify(reportStorage).upload(org.mockito.ArgumentMatchers.contains(".pdf"), any(), org.mockito.ArgumentMatchers.eq("application/pdf"));
    }

    @Test
    void marksTheJobFailedWhenStorageUploadFails() {
        UUID accountId = UUID.randomUUID();
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        when(accountActivityRepository.findByAccountAndPeriod(accountId, from, to)).thenReturn(List.of());
        when(csvStatementRenderer.render(List.of())).thenReturn("csv".getBytes());
        when(pdfStatementRenderer.render(any(StatementJob.class), any())).thenReturn("pdf".getBytes());
        when(reportStorage.upload(anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("S3 unavailable"));

        StatementJob result = useCase.execute(UUID.randomUUID(), accountId, from, to);

        assertThat(result.status()).isEqualTo(StatementStatus.FAILED);
        assertThat(result.csvFileUrl()).isNull();
    }
}
