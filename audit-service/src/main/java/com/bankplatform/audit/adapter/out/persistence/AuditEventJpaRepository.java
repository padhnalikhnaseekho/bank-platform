package com.bankplatform.audit.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, UUID> {

    @Query("SELECT a FROM AuditEventEntity a WHERE "
            + "(:aggregateId IS NULL OR a.aggregateId = :aggregateId) AND "
            + "(:eventType IS NULL OR a.eventType = :eventType) AND "
            + "(CAST(:from AS java.time.Instant) IS NULL OR a.occurredAt >= :from) AND "
            + "(CAST(:to AS java.time.Instant) IS NULL OR a.occurredAt <= :to)")
    Page<AuditEventEntity> search(@Param("aggregateId") String aggregateId, @Param("eventType") String eventType,
            @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
