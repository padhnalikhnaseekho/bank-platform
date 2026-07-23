package com.bankplatform.payment.adapter.in.web;

import com.bankplatform.payment.adapter.in.web.dto.CreateRecurringPaymentRequest;
import com.bankplatform.payment.adapter.in.web.dto.CreateScheduledPaymentRequest;
import com.bankplatform.payment.adapter.in.web.dto.PaymentInstructionResponse;
import com.bankplatform.payment.application.CancelPaymentUseCase;
import com.bankplatform.payment.application.CreateRecurringPaymentUseCase;
import com.bankplatform.payment.application.CreateScheduledPaymentUseCase;
import com.bankplatform.payment.domain.Money;
import com.bankplatform.payment.domain.PaymentId;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final CreateScheduledPaymentUseCase createScheduledPaymentUseCase;
    private final CreateRecurringPaymentUseCase createRecurringPaymentUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;

    public PaymentController(
            CreateScheduledPaymentUseCase createScheduledPaymentUseCase,
            CreateRecurringPaymentUseCase createRecurringPaymentUseCase,
            CancelPaymentUseCase cancelPaymentUseCase) {
        this.createScheduledPaymentUseCase = createScheduledPaymentUseCase;
        this.createRecurringPaymentUseCase = createRecurringPaymentUseCase;
        this.cancelPaymentUseCase = cancelPaymentUseCase;
    }

    @PostMapping("/scheduled")
    public ResponseEntity<PaymentInstructionResponse> createScheduled(
            @Valid @RequestBody CreateScheduledPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        Money amount = Money.of(request.amount(), request.currency());
        var instruction =
                createScheduledPaymentUseCase.execute(
                        customerId,
                        request.sourceAccountId(),
                        request.payeeAccountId(),
                        amount,
                        request.runAt());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentInstructionResponse.from(instruction));
    }

    @PostMapping("/recurring")
    public ResponseEntity<PaymentInstructionResponse> createRecurring(
            @Valid @RequestBody CreateRecurringPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        Money amount = Money.of(request.amount(), request.currency());
        var instruction =
                createRecurringPaymentUseCase.execute(
                        customerId,
                        request.sourceAccountId(),
                        request.payeeAccountId(),
                        amount,
                        request.startAt(),
                        request.intervalDays());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentInstructionResponse.from(instruction));
    }

    @PostMapping("/{paymentId}/cancel")
    public PaymentInstructionResponse cancel(
            @PathVariable UUID paymentId, @AuthenticationPrincipal Jwt jwt) {
        var instruction =
                cancelPaymentUseCase.execute(
                        PaymentId.of(paymentId), UUID.fromString(jwt.getSubject()), isAdmin(jwt));
        return PaymentInstructionResponse.from(instruction);
    }

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
