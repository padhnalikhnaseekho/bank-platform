package com.bankplatform.account.adapter.out.persistence;

import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountId;
import com.bankplatform.account.domain.AccountStatus;
import com.bankplatform.account.domain.LedgerEntry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository accountJpaRepository;
    private final LedgerEntryJpaRepository ledgerEntryJpaRepository;

    public AccountRepositoryAdapter(
            AccountJpaRepository accountJpaRepository,
            LedgerEntryJpaRepository ledgerEntryJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
        this.ledgerEntryJpaRepository = ledgerEntryJpaRepository;
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity =
                accountJpaRepository
                        .findById(account.id().value())
                        .map(
                                existing -> {
                                    AccountMapper.applyToExisting(existing, account);
                                    return existing;
                                })
                        .orElseGet(() -> AccountMapper.toNewEntity(account));
        AccountEntity saved = accountJpaRepository.save(entity);
        return AccountMapper.toDomain(saved);
    }

    @Override
    public void saveLedgerEntry(LedgerEntry entry) {
        ledgerEntryJpaRepository.save(AccountMapper.toEntity(entry));
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return accountJpaRepository.findById(id.value()).map(AccountMapper::toDomain);
    }

    @Override
    public Page<Account> findByCustomerId(
            UUID customerId, AccountStatus status, Pageable pageable) {
        Page<AccountEntity> page =
                status != null
                        ? accountJpaRepository.findByCustomerIdAndStatus(
                                customerId, status.name(), pageable)
                        : accountJpaRepository.findByCustomerId(customerId, pageable);
        return page.map(AccountMapper::toDomain);
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return accountJpaRepository.existsByAccountNumber(accountNumber);
    }
}
