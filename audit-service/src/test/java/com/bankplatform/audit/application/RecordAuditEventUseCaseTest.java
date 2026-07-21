package com.bankplatform.audit.application;

import static org.mockito.Mockito.verify;

import com.bankplatform.audit.application.port.AuditEventRepository;
import com.bankplatform.audit.domain.AuditEvent;
import com.bankplatform.common.event.EventEnvelope;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordAuditEventUseCaseTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    private RecordAuditEventUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RecordAuditEventUseCase(auditEventRepository);
    }

    @Test
    void capturesTheEnvelopeAsAnAuditEvent() {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        EventEnvelope envelope = new EventEnvelope(eventId, "account-created", 1, occurredAt, "account-service",
                "corr-1", null, "Account", "acc-1", "acc-1", null);

        useCase.record(envelope, "{\"a\":1}");

        verify(auditEventRepository).save(argThatMatches(eventId, occurredAt));
    }

    private AuditEvent argThatMatches(UUID eventId, Instant occurredAt) {
        return org.mockito.ArgumentMatchers.argThat(event -> event.eventId().equals(eventId)
                && event.eventType().equals("account-created") && event.aggregateType().equals("Account")
                && event.aggregateId().equals("acc-1") && event.payload().equals("{\"a\":1}")
                && event.correlationId().equals("corr-1") && event.occurredAt().equals(occurredAt));
    }
}
