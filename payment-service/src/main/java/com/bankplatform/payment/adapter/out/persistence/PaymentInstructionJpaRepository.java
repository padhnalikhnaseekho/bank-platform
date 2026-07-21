package com.bankplatform.payment.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentInstructionJpaRepository extends JpaRepository<PaymentInstructionEntity, UUID> {

    List<PaymentInstructionEntity> findByStatusAndNextRunAtLessThanEqualOrderByNextRunAtAsc(String status,
            Instant now, Pageable pageable);
}
