package com.bankplatform.account.adapter.out.persistence;

import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountId;
import com.bankplatform.account.domain.AccountStatus;
import com.bankplatform.account.domain.AccountType;
import com.bankplatform.account.domain.LedgerEntry;
import com.bankplatform.account.domain.Money;
import java.util.Currency;

final class AccountMapper {

    private AccountMapper() {}

    static Account toDomain(AccountEntity entity) {
        Money balance = new Money(entity.getBalance(), Currency.getInstance(entity.getCurrency()));
        return new Account(AccountId.of(entity.getId()), entity.getCustomerId(), entity.getAccountNumber(),
                AccountType.valueOf(entity.getType()), AccountStatus.valueOf(entity.getStatus()), balance,
                entity.getVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    /** For a brand-new account never persisted before. Version is left to Hibernate's default. */
    static AccountEntity toNewEntity(Account account) {
        return new AccountEntity(account.id().value(), account.customerId(), account.accountNumber(),
                account.type().name(), account.status().name(), account.balance().currency().getCurrencyCode(),
                account.balance().amount(), account.createdAt(), account.updatedAt());
    }

    /**
     * Mutates a managed entity in place (rather than building a fresh detached one) so
     * Hibernate's optimistic-lock check on {@code @Version} always compares against the row
     * it just loaded in this transaction, not a stale/ambiguous reconstructed value.
     */
    static void applyToExisting(AccountEntity entity, Account account) {
        entity.applyMutableState(account.status().name(), account.balance().amount(), account.updatedAt());
    }

    static LedgerEntryEntity toEntity(LedgerEntry entry) {
        return new LedgerEntryEntity(entry.id(), entry.accountId().value(), entry.entryType().name(),
                entry.amount().amount(), entry.amount().currency().getCurrencyCode(), entry.referenceId(),
                entry.createdAt());
    }
}
