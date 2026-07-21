package com.bankplatform.transaction.application;

import com.bankplatform.common.error.ConflictException;
import com.bankplatform.transaction.application.port.IdempotencyRepository;
import com.bankplatform.transaction.domain.IdempotencyRecord;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Wraps a create-transaction use case with Idempotency-Key semantics: a replayed key with
 * the same request returns the original response verbatim; the same key with a different
 * request is a conflict. The DB unique index on idempotency_key is the ultimate race
 * guard for genuinely concurrent duplicate requests, not just this up-front check.
 */
@Component
public class IdempotencyGuard {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyGuard(IdempotencyRepository idempotencyRepository, ObjectMapper objectMapper) {
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransactionResult execute(String idempotencyKey, Object requestBody, Supplier<TransactionResult> action) {
        String requestHash = hash(requestBody);
        Optional<IdempotencyRecord> existing = idempotencyRepository.findByKey(idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), requestHash);
        }
        TransactionResult result = action.get();
        String responseJson = objectMapper.writeValueAsString(result);
        try {
            idempotencyRepository.save(IdempotencyRecord.create(idempotencyKey, requestHash, responseJson, 202));
        } catch (DataIntegrityViolationException e) {
            return replay(idempotencyRepository.findByKey(idempotencyKey).orElseThrow(() -> e), requestHash);
        }
        return result;
    }

    private TransactionResult replay(IdempotencyRecord record, String requestHash) {
        if (!record.requestHash().equals(requestHash)) {
            throw new ConflictException("Idempotency-Key already used with a different request");
        }
        return objectMapper.readValue(record.responseBody(), TransactionResult.class);
    }

    private String hash(Object requestBody) {
        String json = objectMapper.writeValueAsString(requestBody);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
