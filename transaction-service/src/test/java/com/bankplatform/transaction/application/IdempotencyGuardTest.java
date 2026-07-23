package com.bankplatform.transaction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.common.error.ConflictException;
import com.bankplatform.transaction.application.port.IdempotencyRepository;
import com.bankplatform.transaction.domain.IdempotencyRecord;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class IdempotencyGuardTest {

    private record RequestBody(String field) {}

    @Mock private IdempotencyRepository idempotencyRepository;

    @Mock private Supplier<TransactionResult> action;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private IdempotencyGuard guard;

    @BeforeEach
    void setUp() {
        guard = new IdempotencyGuard(idempotencyRepository, objectMapper);
    }

    @Test
    void runsActionAndStoresResultForANewKey() {
        when(idempotencyRepository.findByKey("key-1")).thenReturn(Optional.empty());
        TransactionResult result = new TransactionResult(UUID.randomUUID(), "PROCESSING");
        when(action.get()).thenReturn(result);

        TransactionResult actual = guard.execute("key-1", new RequestBody("a"), action);

        assertThat(actual).isEqualTo(result);
        verify(action, times(1)).get();
        verify(idempotencyRepository).save(any(IdempotencyRecord.class));
    }

    @Test
    void replayingTheSameKeyWithTheSameRequestReturnsTheOriginalResultWithoutReRunningTheAction() {
        RequestBody requestBody = new RequestBody("a");
        String requestHash = hash(requestBody);
        TransactionResult originalResult = new TransactionResult(UUID.randomUUID(), "PROCESSING");
        IdempotencyRecord existing =
                IdempotencyRecord.create(
                        "key-1", requestHash, objectMapper.writeValueAsString(originalResult), 202);
        when(idempotencyRepository.findByKey("key-1")).thenReturn(Optional.of(existing));

        TransactionResult actual = guard.execute("key-1", requestBody, action);

        assertThat(actual).isEqualTo(originalResult);
        verify(action, never()).get();
        verify(idempotencyRepository, never()).save(any());
    }

    @Test
    void reusingTheSameKeyWithADifferentRequestIsAConflict() {
        IdempotencyRecord existing =
                IdempotencyRecord.create(
                        "key-1",
                        hash(new RequestBody("original")),
                        objectMapper.writeValueAsString(
                                new TransactionResult(UUID.randomUUID(), "PROCESSING")),
                        202);
        when(idempotencyRepository.findByKey("key-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> guard.execute("key-1", new RequestBody("different"), action))
                .isInstanceOf(ConflictException.class);
        verify(action, never()).get();
    }

    @Test
    void concurrentInsertRaceIsResolvedByReplayingTheWinningRecord() {
        RequestBody requestBody = new RequestBody("a");
        String requestHash = hash(requestBody);
        TransactionResult racersResult = new TransactionResult(UUID.randomUUID(), "PROCESSING");
        IdempotencyRecord racersRecord =
                IdempotencyRecord.create(
                        "key-1", requestHash, objectMapper.writeValueAsString(racersResult), 202);

        when(idempotencyRepository.findByKey("key-1"))
                .thenReturn(Optional.empty(), Optional.of(racersRecord));
        when(action.get()).thenReturn(new TransactionResult(UUID.randomUUID(), "PROCESSING"));
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(idempotencyRepository)
                .save(any());

        TransactionResult actual = guard.execute("key-1", requestBody, action);

        assertThat(actual).isEqualTo(racersResult);
    }

    private String hash(Object requestBody) {
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hashed);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
