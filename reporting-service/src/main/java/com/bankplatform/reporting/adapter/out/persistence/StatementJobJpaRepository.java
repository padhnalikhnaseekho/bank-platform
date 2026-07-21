package com.bankplatform.reporting.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatementJobJpaRepository extends JpaRepository<StatementJobEntity, UUID> {}
