package com.bankplatform.transaction.adapter.in.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bankplatform.common.testfixtures.PostgresTestcontainerBase;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionFlowIntegrationTest extends PostgresTestcontainerBase {

    @Autowired private MockMvc mockMvc;

    @Test
    void depositIsAcceptedAndReplayingTheSameIdempotencyKeyReturnsTheOriginalResult()
            throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String body =
                """
                {"accountId":"%s","amount":50.00,"currency":"INR"}
                """
                        .formatted(accountId);

        String firstResponse =
                mockMvc.perform(
                                post("/api/v1/transactions/deposits")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Idempotency-Key", "dep-key-1")
                                        .content(body)
                                        .with(
                                                jwt().jwt(j -> j.subject(customerId.toString()))
                                                        .authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_CUSTOMER"))))
                        .andExpect(status().isAccepted())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String transactionId = JsonPath.read(firstResponse, "$.transactionId");

        String replayResponse =
                mockMvc.perform(
                                post("/api/v1/transactions/deposits")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Idempotency-Key", "dep-key-1")
                                        .content(body)
                                        .with(
                                                jwt().jwt(j -> j.subject(customerId.toString()))
                                                        .authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_CUSTOMER"))))
                        .andExpect(status().isAccepted())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(
                        (String) JsonPath.read(replayResponse, "$.transactionId"))
                .isEqualTo(transactionId);
    }

    @Test
    void sameIdempotencyKeyWithADifferentRequestBodyIsAConflict() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/transactions/deposits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Idempotency-Key", "dep-key-2")
                                .content(
                                        """
                                {"accountId":"%s","amount":50.00,"currency":"INR"}
                                """
                                                .formatted(accountId))
                                .with(
                                        jwt().jwt(j -> j.subject(customerId.toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isAccepted());

        mockMvc.perform(
                        post("/api/v1/transactions/deposits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Idempotency-Key", "dep-key-2")
                                .content(
                                        """
                                {"accountId":"%s","amount":99.00,"currency":"INR"}
                                """
                                                .formatted(accountId))
                                .with(
                                        jwt().jwt(j -> j.subject(customerId.toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isConflict());
    }

    @Test
    void depositWithoutIdempotencyKeyHeaderIsBadRequest() throws Exception {
        UUID customerId = UUID.randomUUID();
        String body =
                """
                {"accountId":"%s","amount":50.00,"currency":"INR"}
                """
                        .formatted(UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/transactions/deposits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .with(
                                        jwt().jwt(j -> j.subject(customerId.toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void depositWithoutTokenIsUnauthorized() throws Exception {
        String body =
                """
                {"accountId":"%s","amount":50.00,"currency":"INR"}
                """
                        .formatted(UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/transactions/deposits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Idempotency-Key", "dep-key-3")
                                .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void transferToTheSameAccountIsRejected() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String body =
                """
                {"sourceAccountId":"%s","targetAccountId":"%s","amount":10.00,"currency":"INR"}
                """
                        .formatted(accountId, accountId);

        mockMvc.perform(
                        post("/api/v1/transactions/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Idempotency-Key", "xfer-key-1")
                                .content(body)
                                .with(
                                        jwt().jwt(j -> j.subject(customerId.toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerCanViewTheirTransactionButAnotherCustomerCannot() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String body =
                """
                {"accountId":"%s","amount":20.00,"currency":"INR"}
                """
                        .formatted(accountId);

        String response =
                mockMvc.perform(
                                post("/api/v1/transactions/deposits")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Idempotency-Key", "dep-key-4")
                                        .content(body)
                                        .with(
                                                jwt().jwt(j -> j.subject(customerId.toString()))
                                                        .authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_CUSTOMER"))))
                        .andExpect(status().isAccepted())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String transactionId = JsonPath.read(response, "$.transactionId");

        mockMvc.perform(
                        get("/api/v1/transactions/" + transactionId)
                                .with(
                                        jwt().jwt(j -> j.subject(customerId.toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/v1/transactions/" + transactionId)
                                .with(
                                        jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/v1/transactions/" + transactionId)
                                .with(
                                        jwt().jwt(
                                                        j ->
                                                                j.subject(
                                                                                UUID.randomUUID()
                                                                                        .toString())
                                                                        .claim(
                                                                                "roles",
                                                                                java.util.List.of(
                                                                                        "ADMIN")))
                                                .authorities(
                                                        new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }
}
