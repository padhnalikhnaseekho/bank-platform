package com.bankplatform.fraud.adapter.streams;

import com.bankplatform.fraud.application.rule.FraudRule;
import java.time.Duration;
import java.util.List;
import org.apache.kafka.streams.StreamsBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class FraudStreamsConfig {

    public FraudStreamsConfig(
            StreamsBuilder streamsBuilder,
            ObjectMapper objectMapper,
            List<FraudRule> rules,
            @Value("${bank-platform.fraud.window-minutes:10}") long windowMinutes) {
        FraudDetectionTopologyBuilder.build(
                streamsBuilder, objectMapper, rules, Duration.ofMinutes(windowMinutes));
    }
}
