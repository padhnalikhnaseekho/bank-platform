package com.bankplatform.account.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountId;
import com.bankplatform.account.domain.AccountType;
import com.bankplatform.account.domain.Money;
import com.bankplatform.common.event.EventEnvelope;
import com.bankplatform.common.testfixtures.PostgresTestcontainerBase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import tools.jackson.databind.ObjectMapper;

/**
 * Fans many concurrent transfers into a single shared target account — the scenario the
 * "Concurrent transfer tests" checklist item calls for. Each transfer retries on an
 * optimistic-locking conflict (mirroring what Kafka's own redelivery-with-backoff would
 * eventually do in production; see KafkaErrorHandlingAutoConfiguration) so the assertion is
 * on the end state: no concurrent credit is ever silently lost.
 */
@SpringBootTest
class ConcurrentTransferIntegrationTest extends PostgresTestcontainerBase {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferListener transferListener;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private record TransferPayload(String transactionId, String type, String sourceAccountId, String targetAccountId,
            BigDecimal amount, String currency) {}

    @Test
    void manyConcurrentTransfersIntoTheSameAccountAreAllApplied() throws Exception {
        System.out.println("DEBUG transferListener class = " + transferListener.getClass());
        int concurrency = 10;
        BigDecimal perTransferAmount = new BigDecimal("10.00");

        Account target = accountRepository
                .save(Account.open(UUID.randomUUID(), "900000000002", AccountType.SAVINGS, "INR"));
        List<AccountId> sourceIds = IntStream.range(0, concurrency)
                .mapToObj(i -> {
                    Account source = Account.open(UUID.randomUUID(), "90000000010" + i, AccountType.CURRENT, "INR");
                    source.credit(Money.of(new BigDecimal("100.00"), "INR"), "seed");
                    return accountRepository.save(source).id();
                })
                .toList();

        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger failuresNeverRecovered = new AtomicInteger();

        // Fan-out: one virtual thread per transfer (cheap enough to skip a fixed pool
        // entirely), then fan-in by joining every Future before asserting on the end state.
        try (ExecutorService virtualThreadsPerTask = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (AccountId sourceId : sourceIds) {
                futures.add(virtualThreadsPerTask.submit(() -> {
                    ready.countDown();
                    awaitUninterruptibly(start);
                    String message = transferMessage(sourceId.value(), target.id().value(), perTransferAmount);
                    if (!transferWithRetry(message)) {
                        failuresNeverRecovered.incrementAndGet();
                    }
                }));
            }

            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            for (var future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        }

        assertThat(failuresNeverRecovered.get()).isZero();
        Account finalTarget = accountRepository.findById(target.id()).orElseThrow();
        assertThat(finalTarget.balance().amount())
                .isEqualByComparingTo(perTransferAmount.multiply(BigDecimal.valueOf(concurrency)));
        for (AccountId sourceId : sourceIds) {
            Account finalSource = accountRepository.findById(sourceId).orElseThrow();
            assertThat(finalSource.balance().amount()).isEqualByComparingTo("90.00");
        }
    }

    private boolean transferWithRetry(String message) {
        for (int attempt = 0; attempt < 50; attempt++) {
            try {
                transferListener.onTransferStarted(message);
                return true;
            } catch (OptimisticLockingFailureException e) {
                // Simulates the redelivery Kafka's error handler would perform in production.
            }
        }
        return false;
    }

    private String transferMessage(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount) {
        String transactionId = UUID.randomUUID().toString();
        TransferPayload payload = new TransferPayload(transactionId, "TRANSFER", sourceAccountId.toString(),
                targetAccountId.toString(), amount, "INR");
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), "transfer-started", 1, Instant.now(),
                "transaction-service", "corr-1", null, "Transaction", transactionId, transactionId, payload);
        return objectMapper.writeValueAsString(envelope);
    }

    private void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
