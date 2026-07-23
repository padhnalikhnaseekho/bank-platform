package com.bankplatform.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankplatform.common.error.ConflictException;
import com.bankplatform.common.error.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StatementJobTest {

    @Test
    void rejectsAPeriodStartThatIsNotBeforePeriodEnd() {
        Instant instant = Instant.now();

        assertThatThrownBy(
                        () ->
                                StatementJob.request(
                                        UUID.randomUUID(), UUID.randomUUID(), instant, instant))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void requestedJobStartsPending() {
        StatementJob job =
                StatementJob.request(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.now().minusSeconds(3600),
                        Instant.now());

        assertThat(job.status()).isEqualTo(StatementStatus.PENDING);
        assertThat(job.csvFileUrl()).isNull();
        assertThat(job.pdfFileUrl()).isNull();
    }

    @Test
    void completeStoresBothFileUrlsAndMarksCompleted() {
        StatementJob job =
                StatementJob.request(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.now().minusSeconds(3600),
                        Instant.now());

        job.complete("s3://bucket/statement.csv", "s3://bucket/statement.pdf");

        assertThat(job.status()).isEqualTo(StatementStatus.COMPLETED);
        assertThat(job.csvFileUrl()).isEqualTo("s3://bucket/statement.csv");
        assertThat(job.pdfFileUrl()).isEqualTo("s3://bucket/statement.pdf");
    }

    @Test
    void failMarksTheJobFailed() {
        StatementJob job =
                StatementJob.request(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.now().minusSeconds(3600),
                        Instant.now());

        job.fail();

        assertThat(job.status()).isEqualTo(StatementStatus.FAILED);
    }

    @Test
    void rejectsCompletingAJobThatIsNotPending() {
        StatementJob job =
                StatementJob.request(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.now().minusSeconds(3600),
                        Instant.now());
        job.fail();

        assertThatThrownBy(() -> job.complete("csv-url", "pdf-url"))
                .isInstanceOf(ConflictException.class);
    }
}
