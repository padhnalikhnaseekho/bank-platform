package com.bankplatform.common.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotentEventProcessorTest {

    @Mock
    private ProcessedEventStore processedEventStore;

    @Mock
    private Runnable handler;

    private IdempotentEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new IdempotentEventProcessor(processedEventStore);
    }

    @Test
    void runsHandlerAndMarksProcessedWhenNotSeenBefore() {
        UUID eventId = UUID.randomUUID();
        when(processedEventStore.isProcessed(eventId)).thenReturn(false);

        processor.process(eventId, "user-created", handler);

        verify(handler, times(1)).run();
        verify(processedEventStore).markProcessed(eq(eventId), eq("user-created"));
    }

    @Test
    void skipsHandlerAndDoesNotReRecordWhenAlreadyProcessed() {
        UUID eventId = UUID.randomUUID();
        when(processedEventStore.isProcessed(eventId)).thenReturn(true);

        processor.process(eventId, "user-created", handler);

        verify(handler, never()).run();
        verify(processedEventStore, never()).markProcessed(any(), any());
    }
}
