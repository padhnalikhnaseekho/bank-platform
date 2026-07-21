package com.bankplatform.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.audit.application.port.AuditEventRepository;
import com.bankplatform.audit.domain.AuditEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SearchAuditEventsUseCaseTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    private SearchAuditEventsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchAuditEventsUseCase(auditEventRepository);
    }

    @Test
    void delegatesSearchCriteriaToTheRepository() {
        AuditEvent event = AuditEvent.capture(UUID.randomUUID(), "account-created", "Account", "acc-1", "{}", "c-1",
                Instant.now());
        Pageable pageable = PageRequest.of(0, 20);
        Instant from = Instant.now().minusSeconds(60);
        Instant to = Instant.now();
        Page<AuditEvent> page = new PageImpl<>(java.util.List.of(event));
        when(auditEventRepository.search("acc-1", "account-created", from, to, pageable)).thenReturn(page);

        Page<AuditEvent> result = useCase.search("acc-1", "account-created", from, to, pageable);

        assertThat(result.getContent()).containsExactly(event);
        verify(auditEventRepository).search("acc-1", "account-created", from, to, pageable);
    }
}
