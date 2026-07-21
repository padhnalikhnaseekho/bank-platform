package com.bankplatform.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.bankplatform.common.web.CorrelationIdFilter;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherAdapterTest {

    @Mock
    private OutboxRepository outboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OutboxEventPublisherAdapter adapter;

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void savesOutboxRecordWithSerializedPayloadAndNoCorrelationIdWhenAbsent() {
        adapter = new OutboxEventPublisherAdapter(outboxRepository, objectMapper);
        UUID accountId = UUID.randomUUID();

        adapter.publish("account-created", "Account", accountId.toString(), new TestPayload("alice"));

        ArgumentCaptor<OutboxRecord> captor = ArgumentCaptor.forClass(OutboxRecord.class);
        verify(outboxRepository).save(captor.capture());
        OutboxRecord record = captor.getValue();

        assertThat(record.id()).isNotNull();
        assertThat(record.aggregateType()).isEqualTo("Account");
        assertThat(record.aggregateId()).isEqualTo(accountId.toString());
        assertThat(record.eventType()).isEqualTo("account-created");
        assertThat(record.eventVersion()).isEqualTo(1);
        assertThat(record.payloadJson()).contains("\"name\":\"alice\"");
        assertThat(record.correlationId()).isNull();
        assertThat(record.createdAt()).isNotNull();
    }

    @Test
    void carriesCorrelationIdFromMdcWhenPresent() {
        adapter = new OutboxEventPublisherAdapter(outboxRepository, objectMapper);
        MDC.put(CorrelationIdFilter.MDC_KEY, "corr-123");

        adapter.publish("account-created", "Account", UUID.randomUUID().toString(), new TestPayload("bob"));

        ArgumentCaptor<OutboxRecord> captor = ArgumentCaptor.forClass(OutboxRecord.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().correlationId()).isEqualTo("corr-123");
    }

    private record TestPayload(String name) {}
}
