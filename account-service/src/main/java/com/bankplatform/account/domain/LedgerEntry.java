package com.bankplatform.account.domain;

import java.time.Instant;
import java.util.UUID;

public class LedgerEntry {

    public enum EntryType {
        DEBIT,
        CREDIT
    }

    private final UUID id;
    private final AccountId accountId;
    private final EntryType entryType;
    private final Money amount;
    private final String referenceId;
    private final Instant createdAt;

    public LedgerEntry(
            UUID id,
            AccountId accountId,
            EntryType entryType,
            Money amount,
            String referenceId,
            Instant createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.entryType = entryType;
        this.amount = amount;
        this.referenceId = referenceId;
        this.createdAt = createdAt;
    }

    public static LedgerEntry of(
            AccountId accountId, EntryType entryType, Money amount, String referenceId) {
        return new LedgerEntry(
                UUID.randomUUID(), accountId, entryType, amount, referenceId, Instant.now());
    }

    public UUID id() {
        return id;
    }

    public AccountId accountId() {
        return accountId;
    }

    public EntryType entryType() {
        return entryType;
    }

    public Money amount() {
        return amount;
    }

    public String referenceId() {
        return referenceId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
