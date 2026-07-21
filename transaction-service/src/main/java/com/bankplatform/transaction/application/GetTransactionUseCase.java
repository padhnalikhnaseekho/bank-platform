package com.bankplatform.transaction.application;

import com.bankplatform.common.error.NotFoundException;
import com.bankplatform.transaction.application.port.TransactionRepository;
import com.bankplatform.transaction.domain.Transaction;
import com.bankplatform.transaction.domain.TransactionId;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTransactionUseCase {

    private final TransactionRepository transactionRepository;

    public GetTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public Transaction getById(TransactionId id, UUID requesterId, boolean isAdmin) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        if (!isAdmin && !transaction.customerId().equals(requesterId)) {
            throw new AccessDeniedException("Not authorized to view this transaction");
        }
        return transaction;
    }
}
