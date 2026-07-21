package com.bankplatform.payment.adapter.out.persistence;

import com.bankplatform.payment.application.port.PaymentInstructionRepository;
import com.bankplatform.payment.domain.PaymentId;
import com.bankplatform.payment.domain.PaymentInstruction;
import com.bankplatform.payment.domain.PaymentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class PaymentInstructionRepositoryAdapter implements PaymentInstructionRepository {

    private final PaymentInstructionJpaRepository jpaRepository;

    public PaymentInstructionRepositoryAdapter(PaymentInstructionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PaymentInstruction save(PaymentInstruction instruction) {
        PaymentInstructionEntity saved = jpaRepository.save(PaymentInstructionMapper.toEntity(instruction));
        return PaymentInstructionMapper.toDomain(saved);
    }

    @Override
    public Optional<PaymentInstruction> findById(PaymentId id) {
        return jpaRepository.findById(id.value()).map(PaymentInstructionMapper::toDomain);
    }

    @Override
    public List<PaymentInstruction> findDue(Instant now, int limit) {
        return jpaRepository
                .findByStatusAndNextRunAtLessThanEqualOrderByNextRunAtAsc(PaymentStatus.ACTIVE.name(), now,
                        PageRequest.of(0, limit))
                .stream().map(PaymentInstructionMapper::toDomain).toList();
    }
}
