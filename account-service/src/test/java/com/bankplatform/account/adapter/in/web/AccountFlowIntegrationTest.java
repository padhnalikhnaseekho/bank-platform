package com.bankplatform.account.adapter.in.web;

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
class AccountFlowIntegrationTest extends PostgresTestcontainerBase {

    @Autowired private MockMvc mockMvc;

    @Test
    void openGetAndFreezeFlow() throws Exception {
        UUID customerId = UUID.randomUUID();
        String openBody =
                """
                {"type":"SAVINGS","currency":"INR"}
                """;

        String openResponse =
                mockMvc.perform(
                                post("/api/v1/accounts")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(openBody)
                                        .with(
                                                jwt().jwt(j -> j.subject(customerId.toString()))
                                                        .authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_CUSTOMER"))))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String accountId = JsonPath.read(openResponse, "$.id");

        mockMvc.perform(
                        get("/api/v1/accounts/" + accountId)
                                .with(
                                        jwt().jwt(j -> j.subject(customerId.toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/accounts/" + accountId + "/freeze")
                                .with(
                                        jwt().jwt(j -> j.subject(customerId.toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/api/v1/accounts/" + accountId + "/freeze")
                                .with(
                                        jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void anotherCustomerCannotViewSomeoneElsesAccount() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String openBody =
                """
                {"type":"CURRENT","currency":"INR"}
                """;

        String openResponse =
                mockMvc.perform(
                                post("/api/v1/accounts")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(openBody)
                                        .with(
                                                jwt().jwt(j -> j.subject(ownerId.toString()))
                                                        .authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_CUSTOMER"))))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String accountId = JsonPath.read(openResponse, "$.id");

        mockMvc.perform(
                        get("/api/v1/accounts/" + accountId)
                                .with(
                                        jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void openAccountWithoutTokenIsUnauthorized() throws Exception {
        String openBody =
                """
                {"type":"SAVINGS","currency":"INR"}
                """;
        mockMvc.perform(
                        post("/api/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(openBody))
                .andExpect(status().isUnauthorized());
    }
}
