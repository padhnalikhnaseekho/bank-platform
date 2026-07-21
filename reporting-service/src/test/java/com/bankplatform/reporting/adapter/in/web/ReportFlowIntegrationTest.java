package com.bankplatform.reporting.adapter.in.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bankplatform.reporting.application.port.AccountActivityRepository;
import com.bankplatform.reporting.domain.AccountActivityEntry;
import com.bankplatform.reporting.testfixtures.PostgresAndS3TestcontainerBase;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Instant;
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
class ReportFlowIntegrationTest extends PostgresAndS3TestcontainerBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountActivityRepository accountActivityRepository;

    @Test
    void generatesAStatementCoveringSeededActivityAndCanBeFetchedAfterwards() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Instant periodStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant periodEnd = Instant.parse("2026-02-01T00:00:00Z");
        accountActivityRepository.save(AccountActivityEntry.create(customerId, accountId, "DEPOSIT",
                new BigDecimal("100.00"), "INR", Instant.parse("2026-01-15T00:00:00Z")));

        String body = """
                {"accountId":"%s","periodStart":"%s","periodEnd":"%s"}
                """.formatted(accountId, periodStart, periodEnd);

        String response = mockMvc
                .perform(post("/api/v1/reports/statements").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(jwt().jwt(j -> j.subject(customerId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.csvFileUrl").value(org.hamcrest.Matchers.startsWith("s3://")))
                .andExpect(jsonPath("$.pdfFileUrl").value(org.hamcrest.Matchers.startsWith("s3://")))
                .andReturn().getResponse().getContentAsString();
        String statementId = JsonPath.read(response, "$.id");

        mockMvc.perform(get("/api/v1/reports/statements/" + statementId)
                        .with(jwt().jwt(j -> j.subject(customerId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void anotherCustomerCannotViewSomeoneElsesStatement() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String body = """
                {"accountId":"%s","periodStart":"2026-01-01T00:00:00Z","periodEnd":"2026-02-01T00:00:00Z"}
                """.formatted(UUID.randomUUID());

        String response = mockMvc
                .perform(post("/api/v1/reports/statements").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(jwt().jwt(j -> j.subject(ownerId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String statementId = JsonPath.read(response, "$.id");

        mockMvc.perform(get("/api/v1/reports/statements/" + statementId)
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void fetchingAnUnknownStatementIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/reports/statements/" + UUID.randomUUID())
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateStatementWithoutTokenIsUnauthorized() throws Exception {
        String body = """
                {"accountId":"%s","periodStart":"2026-01-01T00:00:00Z","periodEnd":"2026-02-01T00:00:00Z"}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/reports/statements").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }
}
