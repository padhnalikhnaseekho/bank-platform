package com.bankplatform.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bankplatform.common.error.NotFoundException;
import com.bankplatform.reporting.application.port.StatementJobRepository;
import com.bankplatform.reporting.domain.StatementId;
import com.bankplatform.reporting.domain.StatementJob;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class GetStatementJobUseCaseTest {

    @Mock
    private StatementJobRepository statementJobRepository;

    private GetStatementJobUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetStatementJobUseCase(statementJobRepository);
    }

    @Test
    void ownerCanViewTheirOwnStatement() {
        UUID customerId = UUID.randomUUID();
        StatementJob job = StatementJob.request(customerId, UUID.randomUUID(), Instant.now().minusSeconds(60),
                Instant.now());
        when(statementJobRepository.findById(job.id())).thenReturn(Optional.of(job));

        StatementJob result = useCase.getById(job.id(), customerId, false);

        assertThat(result).isSameAs(job);
    }

    @Test
    void adminCanViewAnyStatement() {
        StatementJob job = StatementJob.request(UUID.randomUUID(), UUID.randomUUID(), Instant.now().minusSeconds(60),
                Instant.now());
        when(statementJobRepository.findById(job.id())).thenReturn(Optional.of(job));

        StatementJob result = useCase.getById(job.id(), UUID.randomUUID(), true);

        assertThat(result).isSameAs(job);
    }

    @Test
    void rejectsViewingSomeoneElsesStatement() {
        StatementJob job = StatementJob.request(UUID.randomUUID(), UUID.randomUUID(), Instant.now().minusSeconds(60),
                Instant.now());
        when(statementJobRepository.findById(job.id())).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> useCase.getById(job.id(), UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsAnUnknownStatement() {
        StatementId id = StatementId.newId();
        when(statementJobRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getById(id, UUID.randomUUID(), false))
                .isInstanceOf(NotFoundException.class);
    }
}
