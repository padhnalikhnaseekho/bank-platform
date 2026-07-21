package com.bankplatform.reporting.adapter.in.web;

import com.bankplatform.reporting.adapter.in.web.dto.GenerateStatementRequest;
import com.bankplatform.reporting.adapter.in.web.dto.StatementJobResponse;
import com.bankplatform.reporting.application.GenerateStatementUseCase;
import com.bankplatform.reporting.application.GetStatementJobUseCase;
import com.bankplatform.reporting.domain.StatementId;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final GenerateStatementUseCase generateStatementUseCase;
    private final GetStatementJobUseCase getStatementJobUseCase;

    public ReportController(GenerateStatementUseCase generateStatementUseCase,
            GetStatementJobUseCase getStatementJobUseCase) {
        this.generateStatementUseCase = generateStatementUseCase;
        this.getStatementJobUseCase = getStatementJobUseCase;
    }

    @PostMapping("/statements")
    public ResponseEntity<StatementJobResponse> generateStatement(
            @Valid @RequestBody GenerateStatementRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        var job = generateStatementUseCase.execute(customerId, request.accountId(), request.periodStart(),
                request.periodEnd());
        return ResponseEntity.status(HttpStatus.CREATED).body(StatementJobResponse.from(job));
    }

    @GetMapping("/statements/{statementId}")
    public StatementJobResponse getStatement(@PathVariable UUID statementId, @AuthenticationPrincipal Jwt jwt) {
        var job = getStatementJobUseCase.getById(StatementId.of(statementId), UUID.fromString(jwt.getSubject()),
                isAdmin(jwt));
        return StatementJobResponse.from(job);
    }

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
