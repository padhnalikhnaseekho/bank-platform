package com.bankplatform.transaction.application.event;

public record TransactionStatusChangedPayload(String transactionId, String status) {}
