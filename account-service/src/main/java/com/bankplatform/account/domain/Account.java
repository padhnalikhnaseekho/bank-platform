package com.bankplatform.account.domain;

import com.bankplatform.common.error.ConflictException;
import com.bankplatform.common.error.ValidationException;
import java.time.Instant;
import java.util.UUID;

public class Account {

    private final AccountId id;
    private final UUID customerId;
    private final String accountNumber;
    private final AccountType type;
    private AccountStatus status;
    private Money balance;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public Account(
            AccountId id,
            UUID customerId,
            String accountNumber,
            AccountType type,
            AccountStatus status,
            Money balance,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.status = status;
        this.balance = balance;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Account open(
            UUID customerId, String accountNumber, AccountType type, String currencyCode) {
        Instant now = Instant.now();
        return new Account(
                AccountId.newId(),
                customerId,
                accountNumber,
                type,
                AccountStatus.ACTIVE,
                Money.zero(currencyCode),
                0L,
                now,
                now);
    }

    /**
     * No per-account overdraft limit is modeled yet (plan/DATABASE.md has no such column) — Phase 1
     * disallows any overdraft.
     */
    public LedgerEntry debit(Money amount, String referenceId) {
        requireActive();
        requirePositive(amount);
        Money resultingBalance = balance.subtract(amount);
        if (resultingBalance.isNegative()) {
            throw new ConflictException("Insufficient funds");
        }
        balance = resultingBalance;
        updatedAt = Instant.now();
        return LedgerEntry.of(id, LedgerEntry.EntryType.DEBIT, amount, referenceId);
    }

    public LedgerEntry credit(Money amount, String referenceId) {
        requireActive();
        requirePositive(amount);
        balance = balance.add(amount);
        updatedAt = Instant.now();
        return LedgerEntry.of(id, LedgerEntry.EntryType.CREDIT, amount, referenceId);
    }

    public void freeze() {
        if (status == AccountStatus.CLOSED) {
            throw new ConflictException("Cannot freeze a closed account");
        }
        status = AccountStatus.FROZEN;
        updatedAt = Instant.now();
    }

    public void close() {
        status = AccountStatus.CLOSED;
        updatedAt = Instant.now();
    }

    private void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new ConflictException("Account must be active for this operation");
        }
    }

    private void requirePositive(Money amount) {
        if (!amount.isPositive()) {
            throw new ValidationException("Amount must be greater than zero");
        }
    }

    public AccountId id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public String accountNumber() {
        return accountNumber;
    }

    public AccountType type() {
        return type;
    }

    public AccountStatus status() {
        return status;
    }

    public Money balance() {
        return balance;
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
