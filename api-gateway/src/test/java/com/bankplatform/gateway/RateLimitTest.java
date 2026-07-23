package com.bankplatform.gateway;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitTest {

    private static WireMockServer userServiceStub;

    // No dedicated official Testcontainers Redis module exists (unlike Postgres/Kafka), so a
    // plain GenericContainer is the idiomatic choice here.
    private static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @Autowired private MockMvc mockMvc;

    @BeforeAll
    static void startStub() {
        userServiceStub =
                new WireMockServer(
                        WireMockConfiguration.options().port(0).http2PlainDisabled(true));
        userServiceStub.start();
        redis.start();
    }

    @AfterAll
    static void stopStub() {
        userServiceStub.stop();
        redis.stop();
    }

    @DynamicPropertySource
    static void routeProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "bank-platform.routes.user-service",
                () -> "http://localhost:" + userServiceStub.port());
        registry.add("bank-platform.rate-limit.capacity", () -> "2");
        registry.add("bank-platform.rate-limit.period-seconds", () -> "60");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    void rejectsRequestsBeyondCapacityWithTooManyRequests() throws Exception {
        userServiceStub.stubFor(
                WireMock.post(WireMock.urlEqualTo("/api/v1/users/register"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(201)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"id\":\"stub-user-id\"}")));

        mockMvc.perform(
                        post("/api/v1/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/v1/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/v1/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isTooManyRequests());
    }
}
