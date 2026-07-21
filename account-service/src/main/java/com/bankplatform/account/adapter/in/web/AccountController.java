package com.bankplatform.account.adapter.in.web;

import com.bankplatform.account.adapter.in.web.dto.AccountListResponse;
import com.bankplatform.account.adapter.in.web.dto.AccountResponse;
import com.bankplatform.account.adapter.in.web.dto.OpenAccountRequest;
import com.bankplatform.account.application.FreezeAccountUseCase;
import com.bankplatform.account.application.GetAccountUseCase;
import com.bankplatform.account.application.ListAccountsUseCase;
import com.bankplatform.account.application.OpenAccountUseCase;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountId;
import com.bankplatform.account.domain.AccountStatus;
import com.bankplatform.account.domain.AccountType;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final OpenAccountUseCase openAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;
    private final ListAccountsUseCase listAccountsUseCase;
    private final FreezeAccountUseCase freezeAccountUseCase;

    public AccountController(OpenAccountUseCase openAccountUseCase, GetAccountUseCase getAccountUseCase,
            ListAccountsUseCase listAccountsUseCase, FreezeAccountUseCase freezeAccountUseCase) {
        this.openAccountUseCase = openAccountUseCase;
        this.getAccountUseCase = getAccountUseCase;
        this.listAccountsUseCase = listAccountsUseCase;
        this.freezeAccountUseCase = freezeAccountUseCase;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        Account account = openAccountUseCase.open(customerId, AccountType.valueOf(request.type()),
                request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @GetMapping("/{accountId}")
    public AccountResponse get(@PathVariable UUID accountId, @AuthenticationPrincipal Jwt jwt) {
        Account account = getAccountUseCase.getById(AccountId.of(accountId), UUID.fromString(jwt.getSubject()),
                isAdmin(jwt));
        return AccountResponse.from(account);
    }

    @GetMapping
    public AccountListResponse list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) AccountStatus status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        return AccountListResponse.from(
                listAccountsUseCase.list(customerId, status, PageRequest.of(page, size)));
    }

    @PostMapping("/{accountId}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse freeze(@PathVariable UUID accountId) {
        Account account = freezeAccountUseCase.freeze(AccountId.of(accountId));
        return AccountResponse.from(account);
    }

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
