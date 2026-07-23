package com.bankplatform.account.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    boolean existsByAccountNumber(String accountNumber);

    Page<AccountEntity> findByCustomerIdAndStatus(
            UUID customerId, String status, Pageable pageable);

    Page<AccountEntity> findByCustomerId(UUID customerId, Pageable pageable);
}
