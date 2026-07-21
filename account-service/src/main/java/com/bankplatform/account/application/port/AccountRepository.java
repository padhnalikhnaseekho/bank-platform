package com.bankplatform.account.application.port;

import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountId;
import com.bankplatform.account.domain.AccountStatus;
import com.bankplatform.account.domain.LedgerEntry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountRepository {

    Account save(Account account);

    void saveLedgerEntry(LedgerEntry entry);

    Optional<Account> findById(AccountId id);

    Page<Account> findByCustomerId(UUID customerId, AccountStatus status, Pageable pageable);

    boolean existsByAccountNumber(String accountNumber);
}
