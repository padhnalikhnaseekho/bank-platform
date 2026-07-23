# ADR-0005: Extract `@Transactional` logic into separate beans, never call it via `this`

## Status

Accepted

## Context

Concurrency integration tests in account-service (`AccountOptimisticLockingIntegrationTest`,
`ConcurrentTransferIntegrationTest`) were failing intermittently: balance updates weren't
rolling back on conflict the way `@Transactional` should guarantee. The first diagnosis was
wrong — the transactional methods were `package-private`, so the fix applied was making them
`public`, on the theory that Spring silently ignores `@Transactional` on non-public methods.
That fix did not work.

The real cause, found by checking `TransactionSynchronizationManager.isActualTransactionActive()`
and confirming the listener's runtime class was `TransferListener$SpringCGLIB$0`: the
transactional method was being called as `this.applyTransfer(...)` from another method in the
*same* class. Spring's transaction support (like all Spring AOP) works by wrapping the bean in
a proxy; `this` inside a bean method is the raw target object, not the proxy, so a call through
`this` never passes through the proxy and `@Transactional` (or any other Spring AOP advice) on
that method is silently skipped — regardless of method visibility or whether the proxy is
JDK-dynamic or CGLIB.

## Decision

Any method that needs its own transactional (or otherwise AOP-advised) boundary is extracted
into its own `@Service` bean, injected into the caller, and invoked through that injected
reference rather than `this`. Applied first in account-service
(`ApplyTransferUseCase`, `ApplyMoneyMovementUseCase`), then identically in payment-service
(`TriggerPaymentUseCase`, `ApplyPaymentOutcomeUseCase`), reporting-service
(`RecordAccountActivityUseCase`), and transaction-service (`ApplyTransactionOutcomeUseCase`).
The original listener/job classes became thin dispatch wrappers that just call the new bean.

## Consequences

- Every transactional operation gets a real proxy boundary by construction — there is no
  single-class shortcut available that could reintroduce this bug silently.
- One more class per transactional operation than the original single-class design, which
  also reinforces the hexagonal split in [ADR-0001](0001-hexagonal-architecture-per-service.md):
  these extracted classes are exactly the one-use-case-per-class application layer that pattern
  already called for.
- The wrong "make it public" fix was caught and explicitly reverted (not committed) before the
  real fix was found in payment/reporting/transaction-service, rather than shipping a
  misleading comment alongside code that didn't actually work.
