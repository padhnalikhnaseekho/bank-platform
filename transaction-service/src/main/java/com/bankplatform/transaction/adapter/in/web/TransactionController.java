package com.bankplatform.transaction.adapter.in.web;

import com.bankplatform.transaction.adapter.in.web.dto.DepositRequest;
import com.bankplatform.transaction.adapter.in.web.dto.TransactionResponse;
import com.bankplatform.transaction.adapter.in.web.dto.TransferRequest;
import com.bankplatform.transaction.adapter.in.web.dto.WithdrawalRequest;
import com.bankplatform.transaction.application.CreateDepositUseCase;
import com.bankplatform.transaction.application.CreateTransferUseCase;
import com.bankplatform.transaction.application.CreateWithdrawalUseCase;
import com.bankplatform.transaction.application.GetTransactionUseCase;
import com.bankplatform.transaction.application.IdempotencyGuard;
import com.bankplatform.transaction.application.TransactionResult;
import com.bankplatform.transaction.domain.Money;
import com.bankplatform.transaction.domain.Transaction;
import com.bankplatform.transaction.domain.TransactionId;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final CreateDepositUseCase createDepositUseCase;
    private final CreateWithdrawalUseCase createWithdrawalUseCase;
    private final CreateTransferUseCase createTransferUseCase;
    private final GetTransactionUseCase getTransactionUseCase;
    private final IdempotencyGuard idempotencyGuard;

    public TransactionController(CreateDepositUseCase createDepositUseCase,
            CreateWithdrawalUseCase createWithdrawalUseCase, CreateTransferUseCase createTransferUseCase,
            GetTransactionUseCase getTransactionUseCase, IdempotencyGuard idempotencyGuard) {
        this.createDepositUseCase = createDepositUseCase;
        this.createWithdrawalUseCase = createWithdrawalUseCase;
        this.createTransferUseCase = createTransferUseCase;
        this.getTransactionUseCase = getTransactionUseCase;
        this.idempotencyGuard = idempotencyGuard;
    }

    @PostMapping("/deposits")
    public ResponseEntity<TransactionResult> deposit(@RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DepositRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        Money amount = Money.of(request.amount(), request.currency());
        TransactionResult result = idempotencyGuard.execute(idempotencyKey, request, () -> TransactionResult
                .from(createDepositUseCase.execute(customerId, request.accountId(), amount)));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<TransactionResult> withdraw(@RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WithdrawalRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        Money amount = Money.of(request.amount(), request.currency());
        TransactionResult result = idempotencyGuard.execute(idempotencyKey, request, () -> TransactionResult
                .from(createWithdrawalUseCase.execute(customerId, request.accountId(), amount)));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransactionResult> transfer(@RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        Money amount = Money.of(request.amount(), request.currency());
        TransactionResult result = idempotencyGuard.execute(idempotencyKey, request,
                () -> TransactionResult.from(createTransferUseCase.execute(customerId, request.sourceAccountId(),
                        request.targetAccountId(), amount)));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse get(@PathVariable UUID transactionId, @AuthenticationPrincipal Jwt jwt) {
        Transaction transaction = getTransactionUseCase.getById(TransactionId.of(transactionId),
                UUID.fromString(jwt.getSubject()), isAdmin(jwt));
        return TransactionResponse.from(transaction);
    }

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
