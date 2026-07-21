package com.bankplatform.fraud.adapter.streams;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.fraud.adapter.in.messaging.dto.TransferOutcomeEvent;
import com.bankplatform.fraud.application.rule.FraudRule;
import com.bankplatform.fraud.domain.FraudAlert;
import com.bankplatform.fraud.domain.TransferWindowStats;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.SlidingWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.WindowStore;
import tools.jackson.databind.ObjectMapper;

/**
 * Pure topology-building logic, kept free of Spring so it can be exercised directly with
 * {@code TopologyTestDriver} in tests without a Spring context. {@link FraudStreamsConfig}
 * wires this into the Spring-managed {@link StreamsBuilder}.
 */
public final class FraudDetectionTopologyBuilder {

    public static final String INPUT_TOPIC = "transfer-completed";
    public static final String OUTPUT_TOPIC = "fraud-alert";
    private static final String PRODUCER_NAME = "fraud-service";

    private FraudDetectionTopologyBuilder() {}

    public static void build(StreamsBuilder streamsBuilder, ObjectMapper objectMapper, List<FraudRule> rules,
            Duration window) {
        JsonSerde<TransferOutcomeEvent> transferSerde = new JsonSerde<>(TransferOutcomeEvent.class, objectMapper);
        JsonSerde<TransferWindowStats> statsSerde = new JsonSerde<>(TransferWindowStats.class, objectMapper);

        KStream<String, String> transfers = streamsBuilder.stream(INPUT_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String()));

        KStream<String, TransferOutcomeEvent> outgoingTransfers = transfers
                .mapValues(value -> extractPayload(objectMapper, value))
                .filter((transactionId, event) -> event != null && event.sourceCustomerId() != null)
                .selectKey((transactionId, event) -> event.sourceCustomerId());

        KTable<Windowed<String>, TransferWindowStats> windowedStats = outgoingTransfers
                .groupByKey(Grouped.with(Serdes.String(), transferSerde))
                .windowedBy(SlidingWindows.ofTimeDifferenceWithNoGrace(window))
                .aggregate(TransferWindowStats::zero,
                        (customerId, event, stats) -> stats.plus(event.amount(), event.currency()),
                        Materialized
                                .<String, TransferWindowStats, WindowStore<Bytes, byte[]>>as(
                                        "transfer-window-stats-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(statsSerde));

        windowedStats.toStream()
                .flatMap((windowedKey, stats) -> evaluateRules(rules, windowedKey, stats))
                .mapValues(alert -> toEnvelopeJson(objectMapper, alert))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));
    }

    private static Iterable<org.apache.kafka.streams.KeyValue<String, FraudAlert>> evaluateRules(
            List<FraudRule> rules, Windowed<String> windowedKey, TransferWindowStats stats) {
        String customerId = windowedKey.key();
        Instant windowStart = windowedKey.window().startTime();
        Instant windowEnd = windowedKey.window().endTime();
        return rules.stream()
                .flatMap(rule -> rule.evaluate(customerId, stats, windowStart, windowEnd).stream())
                .map(alert -> org.apache.kafka.streams.KeyValue.pair(customerId, alert))
                .toList();
    }

    private static TransferOutcomeEvent extractPayload(ObjectMapper objectMapper, String message) {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        return objectMapper.convertValue(envelope.payload(), TransferOutcomeEvent.class);
    }

    private static String toEnvelopeJson(ObjectMapper objectMapper, FraudAlert alert) {
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), "fraud-alert", 1, alert.triggeredAt(),
                PRODUCER_NAME, null, null, "Customer", alert.customerId(), alert.customerId(), alert);
        return objectMapper.writeValueAsString(envelope);
    }
}
