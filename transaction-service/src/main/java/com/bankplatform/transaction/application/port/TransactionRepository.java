package com.bankplatform.transaction.application.port;

import com.bankplatform.transaction.domain.Transaction;
import com.bankplatform.transaction.domain.TransactionId;
import java.util.Optional;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(TransactionId id);
}
