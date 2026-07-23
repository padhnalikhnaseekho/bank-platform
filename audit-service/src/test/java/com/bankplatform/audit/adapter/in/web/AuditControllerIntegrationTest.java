package com.bankplatform.audit.adapter.in.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bankplatform.audit.application.RecordAuditEventUseCase;
import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.testfixtures.PostgresTestcontainerBase;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuditControllerIntegrationTest extends PostgresTestcontainerBase {

    @Autowired private MockMvc mockMvc;

    @Autowired private RecordAuditEventUseCase recordAuditEventUseCase;

    @Test
    void adminCanSearchRecordedAuditEvents() throws Exception {
        String aggregateId = UUID.randomUUID().toString();
        EventEnvelope envelope =
                new EventEnvelope(
                        UUID.randomUUID(),
                        "account-created",
                        1,
                        Instant.now(),
                        "account-service",
                        "corr-1",
                        null,
                        "Account",
                        aggregateId,
                        aggregateId,
                        null);
        recordAuditEventUseCase.record(envelope, "{\"balance\":0}");

        mockMvc.perform(
                        get("/api/v1/audit/events")
                                .queryParam("aggregateId", aggregateId)
                                .with(
                                        jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].aggregateId").value(aggregateId))
                .andExpect(jsonPath("$.items[0].eventType").value("account-created"));
    }

    @Test
    void nonAdminCannotSearchAuditEvents() throws Exception {
        mockMvc.perform(
                        get("/api/v1/audit/events")
                                .with(
                                        jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events")).andExpect(status().isUnauthorized());
    }
}
