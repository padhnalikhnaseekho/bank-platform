package com.bankplatform.payment.adapter.in.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class PaymentFlowIntegrationTest extends PostgresTestcontainerBase {

    @Autowired private MockMvc mockMvc;

    @Test
    void createsAScheduledPaymentAndCancelsIt() throws Exception {
        UUID customerId = UUID.randomUUID();
        String body =
                """
                {"sourceAccountId":"%s","payeeAccountId":"%s","amount":50.00,"currency":"INR",
                 "runAt":"2099-01-01T00:00:00Z"}
                """
                        .formatted(UUID.randomUUID(), UUID.randomUUID());

        String response =
                mockMvc.perform(
                                post("/api/v1/payments/scheduled")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body)
                                        .with(
                                                jwt().jwt(j -> j.subject(customerId.toString()))
                                                        .authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_CUSTOMER"))))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.scheduleType").value("ONE_TIME"))
                        .andExpect(jsonPath("$.status").value("ACTIVE"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String paymentId = JsonPath.read(response, "$.id");

        mockMvc.perform(
                        post("/api/v1/payments/" + paymentId + "/cancel")
                                .with(
                                        jwt().jwt(j -> j.subject(customerId.toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void createsARecurringPayment() throws Exception {
        UUID customerId = UUID.randomUUID();
        String body =
                """
                {"sourceAccountId":"%s","payeeAccountId":"%s","amount":1000.00,"currency":"INR",
                 "startAt":"2099-01-01T00:00:00Z","intervalDays":30}
                """
                        .formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/payments/recurring")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .with(
                                        jwt().jwt(j -> j.subject(customerId.toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleType").value("RECURRING"))
                .andExpect(jsonPath("$.intervalDays").value(30));
    }

    @Test
    void rejectsSchedulingAPaymentToTheSameAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        String body =
                """
                {"sourceAccountId":"%s","payeeAccountId":"%s","amount":50.00,"currency":"INR",
                 "runAt":"2099-01-01T00:00:00Z"}
                """
                        .formatted(accountId, accountId);

        mockMvc.perform(
                        post("/api/v1/payments/scheduled")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .with(
                                        jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsARunAtInThePast() throws Exception {
        String body =
                """
                {"sourceAccountId":"%s","payeeAccountId":"%s","amount":50.00,"currency":"INR",
                 "runAt":"2020-01-01T00:00:00Z"}
                """
                        .formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/payments/scheduled")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .with(
                                        jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherCustomerCannotCancelSomeoneElsesPayment() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String body =
                """
                {"sourceAccountId":"%s","payeeAccountId":"%s","amount":50.00,"currency":"INR",
                 "runAt":"2099-01-01T00:00:00Z"}
                """
                        .formatted(UUID.randomUUID(), UUID.randomUUID());

        String response =
                mockMvc.perform(
                                post("/api/v1/payments/scheduled")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body)
                                        .with(
                                                jwt().jwt(j -> j.subject(ownerId.toString()))
                                                        .authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_CUSTOMER"))))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String paymentId = JsonPath.read(response, "$.id");

        mockMvc.perform(
                        post("/api/v1/payments/" + paymentId + "/cancel")
                                .with(
                                        jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancellingAnUnknownPaymentIsNotFound() throws Exception {
        mockMvc.perform(
                        post("/api/v1/payments/" + UUID.randomUUID() + "/cancel")
                                .with(
                                        jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void createScheduledPaymentWithoutTokenIsUnauthorized() throws Exception {
        String body =
                """
                {"sourceAccountId":"%s","payeeAccountId":"%s","amount":50.00,"currency":"INR",
                 "runAt":"2099-01-01T00:00:00Z"}
                """
                        .formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/payments/scheduled")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnauthorized());
    }
}
