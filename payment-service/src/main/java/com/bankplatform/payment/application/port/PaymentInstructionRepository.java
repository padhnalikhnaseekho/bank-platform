package com.bankplatform.payment.application.port;

import com.bankplatform.payment.domain.PaymentInstruction;
import com.bankplatform.payment.domain.PaymentId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentInstructionRepository {

    PaymentInstruction save(PaymentInstruction instruction);

    Optional<PaymentInstruction> findById(PaymentId id);

    List<PaymentInstruction> findDue(Instant now, int limit);
}
