package com.bankplatform.transaction.application.port;

import com.bankplatform.transaction.domain.IdempotencyRecord;
import java.util.Optional;

public interface IdempotencyRepository {

    Optional<IdempotencyRecord> findByKey(String idempotencyKey);

    void save(IdempotencyRecord record);
}
