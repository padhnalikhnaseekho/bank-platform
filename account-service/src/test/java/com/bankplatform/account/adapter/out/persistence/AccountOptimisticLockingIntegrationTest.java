package com.bankplatform.account.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankplatform.account.application.port.AccountRepository;
import com.bankplatform.account.domain.Account;
import com.bankplatform.account.domain.AccountId;
import com.bankplatform.account.domain.AccountType;
import com.bankplatform.account.domain.Money;
import com.bankplatform.common.testfixtures.PostgresTestcontainerBase;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Proves the {@code @Version}-based optimistic locking documented in
 * AccountRepositoryAdapter/AccountMapper actually rejects a stale concurrent write instead of
 * silently losing it, using two genuinely overlapping transactions (synchronized on a barrier so
 * both load the row before either commits).
 */
@SpringBootTest
class AccountOptimisticLockingIntegrationTest extends PostgresTestcontainerBase {

    @Autowired private AccountRepository accountRepository;

    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void exactlyOneOfTwoConcurrentUpdatesToTheSameAccountSucceeds() throws Exception {
        Account account =
                accountRepository.save(
                        Account.open(
                                UUID.randomUUID(), "900000000001", AccountType.SAVINGS, "INR"));
        AccountId id = account.id();

        CyclicBarrier bothLoaded = new CyclicBarrier(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Runnable creditTenRupees =
                () ->
                        new TransactionTemplate(transactionManager)
                                .executeWithoutResult(
                                        status -> {
                                            Account loaded =
                                                    accountRepository.findById(id).orElseThrow();
                                            awaitUninterruptibly(bothLoaded);
                                            loaded.credit(
                                                    Money.of(new BigDecimal("10.00"), "INR"),
                                                    "concurrency-test");
                                            accountRepository.save(loaded);
                                        });

        Thread t1 = runInThread(creditTenRupees, successCount, failure);
        Thread t2 = runInThread(creditTenRupees, successCount, failure);
        t1.join();
        t2.join();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failure.get()).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        Account finalState = accountRepository.findById(id).orElseThrow();
        assertThat(finalState.balance().amount()).isEqualByComparingTo("10.00");
    }

    private Thread runInThread(
            Runnable task, AtomicInteger successCount, AtomicReference<Throwable> failure) {
        Thread thread =
                Thread.ofVirtual()
                        .unstarted(
                                () -> {
                                    try {
                                        task.run();
                                        successCount.incrementAndGet();
                                    } catch (Throwable t) {
                                        failure.set(t);
                                    }
                                });
        thread.start();
        return thread;
    }

    private void awaitUninterruptibly(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
