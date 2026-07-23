package com.bankplatform.audit.adapter.in.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.bankplatform.audit.application.RecordAuditEventUseCase;
import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.event.IdempotentEventProcessor;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock private RecordAuditEventUseCase recordAuditEventUseCase;

    @Mock private IdempotentEventProcessor idempotentEventProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuditEventListener listener;

    private record SamplePayload(String field) {}

    @BeforeEach
    void setUp() {
        listener =
                new AuditEventListener(
                        recordAuditEventUseCase, idempotentEventProcessor, objectMapper);
    }

    private void stubProcessorToRunHandler() {
        doAnswer(
                        invocation -> {
                            Runnable handler = invocation.getArgument(2);
                            handler.run();
                            return null;
                        })
                .when(idempotentEventProcessor)
                .process(any(), any(), any());
    }

    @Test
    void recordsEveryIncomingEventRegardlessOfType() {
        stubProcessorToRunHandler();
        UUID eventId = UUID.randomUUID();
        EventEnvelope envelope =
                new EventEnvelope(
                        eventId,
                        "account-created",
                        1,
                        Instant.now(),
                        "account-service",
                        "corr-1",
                        null,
                        "Account",
                        "acc-1",
                        "acc-1",
                        new SamplePayload("value"));
        String message = objectMapper.writeValueAsString(envelope);

        listener.onEvent(message);

        ArgumentCaptor<EventEnvelope> envelopeCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        ArgumentCaptor<String> payloadJsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(recordAuditEventUseCase)
                .record(envelopeCaptor.capture(), payloadJsonCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(envelopeCaptor.getValue().eventId())
                .isEqualTo(eventId);
        org.assertj.core.api.Assertions.assertThat(payloadJsonCaptor.getValue())
                .contains("\"field\":\"value\"");
    }

    @Test
    void skipsAlreadyProcessedEvents() {
        EventEnvelope envelope =
                new EventEnvelope(
                        UUID.randomUUID(),
                        "account-created",
                        1,
                        Instant.now(),
                        "account-service",
                        "corr-1",
                        null,
                        "Account",
                        "acc-1",
                        "acc-1",
                        new SamplePayload("value"));
        String message = objectMapper.writeValueAsString(envelope);

        listener.onEvent(message);

        verify(recordAuditEventUseCase, org.mockito.Mockito.never()).record(any(), any());
    }
}
