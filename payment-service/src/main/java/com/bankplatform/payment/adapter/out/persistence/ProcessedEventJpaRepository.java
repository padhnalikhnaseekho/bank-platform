package com.bankplatform.payment.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, UUID> {}
