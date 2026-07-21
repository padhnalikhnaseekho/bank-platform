package com.bankplatform.transaction.domain;

public enum TransactionStatus {
    RECEIVED,
    VALIDATED,
    PROCESSING,
    COMPLETED,
    FAILED,
    REVERSED
}
