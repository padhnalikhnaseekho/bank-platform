package com.bankplatform.fraud.adapter.streams;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.ObjectMapper;

/** Minimal Jackson-backed Serde — matches the rest of the codebase's plain-JSON-over-Kafka convention. */
public class JsonSerde<T> implements Serde<T> {

    private final Class<T> type;
    private final ObjectMapper objectMapper;

    public JsonSerde(Class<T> type, ObjectMapper objectMapper) {
        this.type = type;
        this.objectMapper = objectMapper;
    }

    @Override
    public Serializer<T> serializer() {
        return (topic, data) -> data == null ? null
                : objectMapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Deserializer<T> deserializer() {
        return (topic, data) -> data == null ? null
                : objectMapper.readValue(new String(data, StandardCharsets.UTF_8), type);
    }
}
