package com.bankplatform.fraud.adapter.streams;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.fraud.application.rule.HighTransferCountRule;
import com.bankplatform.fraud.application.rule.HighTransferValueRule;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class FraudDetectionTopologyBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TopologyTestDriver driver;
    private TestInputTopic<String, String> input;
    private TestOutputTopic<String, String> output;

    private record TransferOutcomePayload(
            String transactionId,
            String sourceAccountId,
            String targetAccountId,
            String sourceCustomerId,
            String targetCustomerId,
            BigDecimal amount,
            String currency,
            String failureReason) {}

    private void startDriver(long maxTransferCount, String maxTransferAmount) {
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        FraudDetectionTopologyBuilder.build(
                streamsBuilder,
                objectMapper,
                List.of(
                        new HighTransferCountRule(maxTransferCount),
                        new HighTransferValueRule(new BigDecimal(maxTransferAmount))),
                Duration.ofMinutes(10));
        Topology topology = streamsBuilder.build();

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "fraud-service-test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");

        driver = new TopologyTestDriver(topology, props);
        input =
                driver.createInputTopic(
                        FraudDetectionTopologyBuilder.INPUT_TOPIC,
                        new StringSerializer(),
                        new StringSerializer());
        output =
                driver.createOutputTopic(
                        FraudDetectionTopologyBuilder.OUTPUT_TOPIC,
                        new StringDeserializer(),
                        new StringDeserializer());
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.close();
        }
    }

    private String transferCompletedMessage(String sourceCustomerId, BigDecimal amount) {
        TransferOutcomePayload payload =
                new TransferOutcomePayload(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        sourceCustomerId,
                        UUID.randomUUID().toString(),
                        amount,
                        "INR",
                        null);
        EventEnvelope envelope =
                new EventEnvelope(
                        UUID.randomUUID(),
                        "transfer-completed",
                        1,
                        Instant.now(),
                        "account-service",
                        "corr-1",
                        null,
                        "Transfer",
                        payload.transactionId(),
                        payload.transactionId(),
                        payload);
        return objectMapper.writeValueAsString(envelope);
    }

    @Test
    void emitsHighTransferCountAlertWhenACustomerExceedsTheCountThreshold() {
        startDriver(3, "1000000");
        String customerId = UUID.randomUUID().toString();
        Instant base = Instant.parse("2026-01-01T00:00:00Z");

        for (int i = 0; i < 4; i++) {
            input.advanceTime(Duration.ofSeconds(1));
            input.pipeInput(
                    UUID.randomUUID().toString(),
                    transferCompletedMessage(customerId, new BigDecimal("10")));
        }

        List<String> alerts = output.readValuesToList();
        assertThat(alerts)
                .anySatisfy(
                        json ->
                                assertThat(json)
                                        .contains("HIGH_TRANSFER_COUNT")
                                        .contains(customerId));
    }

    @Test
    void emitsHighTransferValueAlertWhenOutgoingTotalExceedsTheAmountThreshold() {
        startDriver(100, "500");
        String customerId = UUID.randomUUID().toString();

        input.pipeInput(
                UUID.randomUUID().toString(),
                transferCompletedMessage(customerId, new BigDecimal("300")));
        input.advanceTime(Duration.ofSeconds(1));
        input.pipeInput(
                UUID.randomUUID().toString(),
                transferCompletedMessage(customerId, new BigDecimal("300")));

        List<String> alerts = output.readValuesToList();
        assertThat(alerts)
                .anySatisfy(
                        json ->
                                assertThat(json)
                                        .contains("HIGH_TRANSFER_VALUE")
                                        .contains(customerId));
        assertThat(alerts).noneMatch(json -> json.contains("HIGH_TRANSFER_COUNT"));
    }

    @Test
    void doesNotEmitAnyAlertForOrdinaryActivity() {
        startDriver(5, "50000");
        String customerId = UUID.randomUUID().toString();

        input.pipeInput(
                UUID.randomUUID().toString(),
                transferCompletedMessage(customerId, new BigDecimal("100")));
        input.advanceTime(Duration.ofSeconds(1));
        input.pipeInput(
                UUID.randomUUID().toString(),
                transferCompletedMessage(customerId, new BigDecimal("100")));

        assertThat(output.isEmpty()).isTrue();
    }

    @Test
    void ignoresTransfersWithNoSourceCustomerId() {
        startDriver(1, "1");
        TransferOutcomePayload payload =
                new TransferOutcomePayload(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        null,
                        null,
                        new BigDecimal("999999"),
                        "INR",
                        "Account not found");
        EventEnvelope envelope =
                new EventEnvelope(
                        UUID.randomUUID(),
                        "transfer-completed",
                        1,
                        Instant.now(),
                        "account-service",
                        "corr-1",
                        null,
                        "Transfer",
                        payload.transactionId(),
                        payload.transactionId(),
                        payload);

        input.pipeInput(UUID.randomUUID().toString(), objectMapper.writeValueAsString(envelope));

        assertThat(output.isEmpty()).isTrue();
    }
}
