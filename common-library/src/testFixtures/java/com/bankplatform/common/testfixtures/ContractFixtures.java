package com.bankplatform.common.testfixtures;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Golden JSON fixtures for the wire shape of cross-service Kafka events, one file per event
 * type under {@code contracts/}. Producer-side tests assert their actual serialized payload
 * matches the fixture; consumer-side tests deserialize the fixture with their own DTO and
 * assert the fields they declare come through correctly. A fixture change forces both sides
 * of an event contract to be looked at deliberately, instead of drifting apart silently.
 */
public final class ContractFixtures {

    private ContractFixtures() {}

    public static String read(String eventType) {
        String resourcePath = "/contracts/" + eventType + ".json";
        try (InputStream in = ContractFixtures.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("No contract fixture found at " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Asserts that a producer's actual payload matches the named contract fixture. Compares
     * canonicalized JSON strings rather than raw {@code JsonNode} trees: plain
     * {@code ObjectMapper().readTree(...)} parses integers/decimals into whichever node
     * subtype fits (IntNode vs LongNode, double vs BigDecimal), and Jackson's node
     * {@code equals()} treats those as unequal even when the values are numerically identical
     * — e.g. {@code IntNode(6)} does not equal {@code LongNode(6)}. Serializing both sides back
     * to a property-sorted JSON string sidesteps that entirely.
     */
    public static void assertMatchesFixture(String eventType, Object actualPayload) {
        ObjectMapper mapper = canonicalMapper();
        String actual = mapper.writeValueAsString(mapper.valueToTree(actualPayload));
        String expected = mapper.writeValueAsString(mapper.readTree(read(eventType)));
        if (!actual.equals(expected)) {
            throw new AssertionError(
                    "Payload does not match contract fixture '" + eventType + "'.\nExpected: " + expected
                            + "\nActual:   " + actual);
        }
    }

    private static ObjectMapper canonicalMapper() {
        return JsonMapper.builder().enable(JsonNodeFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(JsonNodeFeature.WRITE_PROPERTIES_SORTED).build();
    }
}
