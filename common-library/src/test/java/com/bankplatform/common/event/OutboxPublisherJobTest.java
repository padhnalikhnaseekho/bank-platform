package com.bankplatform.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherJobTest {

    @Mock private OutboxRepository outboxRepository;

    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OutboxPublisherJob job;

    private OutboxRecord record(String eventType, String aggregateId) {
        return new OutboxRecord(
                UUID.randomUUID(),
                "Account",
                aggregateId,
                eventType,
                1,
                "{\"a\":1}",
                "corr-1",
                Instant.now());
    }

    @Test
    void publishesEachPendingRecordAndMarksItPublishedOnSuccess() {
        job =
                new OutboxPublisherJob(
                        outboxRepository, kafkaTemplate, objectMapper, "account-service");
        OutboxRecord pending = record("account-created", "acc-1");
        when(outboxRepository.findPendingBatch(50)).thenReturn(List.of(pending));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mockSendResult()));

        job.publishPending();

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), anyString());
        assertThat(topicCaptor.getValue()).isEqualTo("account-created");
        assertThat(keyCaptor.getValue()).isEqualTo("acc-1");
        verify(outboxRepository).markPublished(pending.id());
        verify(outboxRepository, never()).markFailed(any(), any());
    }

    @Test
    void marksRecordFailedWhenKafkaSendFails() {
        job =
                new OutboxPublisherJob(
                        outboxRepository, kafkaTemplate, objectMapper, "account-service");
        OutboxRecord pending = record("account-created", "acc-2");
        when(outboxRepository.findPendingBatch(50)).thenReturn(List.of(pending));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(
                        CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));

        job.publishPending();

        verify(outboxRepository).markFailed(eq(pending.id()), anyString());
        verify(outboxRepository, never()).markPublished(any());
    }

    @Test
    void marksRecordFailedWhenKafkaSendHangsPastTheTimeout() {
        job =
                new OutboxPublisherJob(
                        outboxRepository,
                        kafkaTemplate,
                        objectMapper,
                        "account-service",
                        Duration.ofMillis(50));
        OutboxRecord pending = record("account-created", "acc-3");
        when(outboxRepository.findPendingBatch(50)).thenReturn(List.of(pending));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(new CompletableFuture<>());

        job.publishPending();

        verify(outboxRepository).markFailed(eq(pending.id()), eq("TimeoutException"));
        verify(outboxRepository, never()).markPublished(any());
    }

    @SuppressWarnings("unchecked")
    private SendResult<String, String> mockSendResult() {
        return org.mockito.Mockito.mock(SendResult.class);
    }
}
