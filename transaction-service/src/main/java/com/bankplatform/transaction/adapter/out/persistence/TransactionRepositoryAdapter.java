package com.bankplatform.transaction.adapter.out.persistence;

import com.bankplatform.transaction.application.port.TransactionRepository;
import com.bankplatform.transaction.domain.Transaction;
import com.bankplatform.transaction.domain.TransactionId;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository jpaRepository;

    public TransactionRepositoryAdapter(TransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity saved = jpaRepository.save(TransactionMapper.toEntity(transaction));
        return TransactionMapper.toDomain(saved);
    }

    @Override
    public Optional<Transaction> findById(TransactionId id) {
        return jpaRepository.findById(id.value()).map(TransactionMapper::toDomain);
    }
}
