package com.bankplatform.gateway;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@SpringBootTest
@AutoConfigureMockMvc
class GatewayRoutingTest {

    private static WireMockServer userServiceStub;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void startStub() {
        // Disable h2c so the JVM's HTTP client (which prefers HTTP/2) doesn't attempt an
        // upgrade WireMock resets mid-stream.
        userServiceStub = new WireMockServer(WireMockConfiguration.options().port(0).http2PlainDisabled(true));
        userServiceStub.start();
    }

    @AfterAll
    static void stopStub() {
        userServiceStub.stop();
    }

    @DynamicPropertySource
    static void routeProperties(DynamicPropertyRegistry registry) {
        registry.add("bank-platform.routes.user-service", () -> "http://localhost:" + userServiceStub.port());
        registry.add("bank-platform.routes.account-service", () -> "http://localhost:" + userServiceStub.port());
    }

    @Test
    void forwardsPublicRegisterRequestToUserService() throws Exception {
        userServiceStub.stubFor(WireMock.post(WireMock.urlEqualTo("/api/v1/users/register"))
                .willReturn(WireMock.aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"stub-user-id\"}")));

        mockMvc.perform(post("/api/v1/users/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
    }

    @Test
    void deniesUnauthenticatedAccountRequest() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")).andExpect(status().isUnauthorized());
    }
}
