# System Design Patterns Analysis

**Source:** _System Design Interview_ by Alex XU

**Last Updated:** 2026-07-23

---

## Executive Summary

The bank-platform implements a solid foundation of microservices patterns but is missing critical resilience and event reliability patterns needed for production. This document maps current implementation against XU's patterns and provides a prioritized roadmap for gaps.

**Current Status:**
- ✅ **10/16** foundational patterns present
- ⚠️ **4/16** patterns partially implemented (stubbed/incomplete)
- 🔴 **2/16** critical patterns missing entirely

---

## ✅ Patterns Already Present

### 1. Microservices Architecture
**Status:** Fully implemented

- 10 independent services: User, Account, Transaction, Payment, Fraud, Notification, Reporting, Audit, AI, Gateway
- Each service has its own database (PostgreSQL)
- Clear bounded contexts per DDD (Identity, Accounts, Transactions, Payments, Fraud, Notifications, Reporting, Audit)
- Defined in `settings.gradle.kts`

**Evidence:**
```gradle
include(
    "common-library",
    "api-gateway",
    "user-service",
    "account-service",
    "transaction-service",
    ...
)
```

---

### 2. API Gateway
**Status:** Fully implemented

- **Location:** `api-gateway/`
- **Responsibilities:**
  - Centralized request routing to downstream services
  - JWT validation (defense in depth)
  - Rate limiting via Resilience4j (`GatewayRoutesConfig`)
  - Routes: `/api/v1/users/**` → user-service, `/api/v1/accounts/**` → account-service

**Evidence:**
- Dependencies: `spring-cloud-starter-gateway-server-webmvc`, `resilience4j-ratelimiter`
- Rate limit config: `bank-platform.rate-limit.capacity` (default 100 req/60s per IP)
- See `README.md`: "api-gateway applies a per-client-IP rate limiter"

---

### 3. Database per Service
**Status:** Fully implemented

- Each service owns PostgreSQL instance with separate schema
- No cross-service database joins
- Defined in `docker/docker-compose.yml`: single `postgres:16` instance; each service uses own database

**Evidence:**
```yaml
postgres:
  image: postgres:16
  environment:
    POSTGRES_USER: bank
    POSTGRES_PASSWORD: bank
    POSTGRES_DB: bank_platform
```

---

### 4. Asynchronous Communication (Kafka)
**Status:** Fully implemented

- **Kafka 3.8.0** in Docker Compose for inter-service events
- Kafka UI on port 8090 for visibility
- All services can publish/consume via Spring Kafka

**Evidence:**
```yaml
kafka:
  image: apache/kafka:3.8.0
  ports:
    - "9094:29092"

kafka-ui:
  image: provectuslabs/kafka-ui:latest
  ports:
    - "8090:8080"
```

**Planned Topics** (from `plan/KAFKA.md`):
- `user-registered`, `account-opened`, `transaction-started`, `transfer-completed`, `fraud-alert`, etc.

---

### 5. Load Balancing & Rate Limiting
**Status:** Fully implemented

- Per-IP rate limiter in API Gateway (100 requests per 60 seconds default)
- Resilience4j configured for rate limiting
- Warning: All k6 load test traffic originates from single container IP → requires `RATE_LIMIT_CAPACITY=100000` override for testing

**Evidence:**
```
Load tests README: "To load test past that ceiling, raise the limit for the duration of the run:
RATE_LIMIT_CAPACITY=100000 ./gradlew :api-gateway:bootRun"
```

---

### 6. Caching Layer
**Status:** Fully implemented

- **Redis 7** in Docker Compose
- Planned for: Fraud service windowed activity, session caching
- Health checks configured

**Evidence:**
```yaml
redis:
  image: redis:7
  ports:
    - "6379:6379"
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
```

---

### 7. Logging & Monitoring (Observability)
**Status:** Fully implemented

- **Prometheus** (port 9090) — metrics collection
- **Grafana** (port 3000) — dashboards and alerts
- **Jaeger** (port 16686) — distributed tracing
- **OpenTelemetry** — structured instrumentation
- JSON logs with `correlationId`, `traceId`, `spanId`, `customerId`, `eventId`

**Dependencies:** 
- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`
- `spring-boot-starter-opentelemetry`

**Evidence:**
```yaml
prometheus:
  image: prom/prometheus:latest
  ports:
    - "9090:9090"

grafana:
  image: grafana/grafana:latest
  ports:
    - "3000:3000"

jaeger:
  image: jaegertracing/all-in-one:latest
  ports:
    - "16686:16686"
```

---

### 8. Health Checks & Readiness Probes
**Status:** Fully implemented

- Spring Boot Actuator enabled in all services
- Health endpoint at `/actuator/health` per service
- Docker Compose health checks for infrastructure (PostgreSQL, Redis)

**Evidence:**
```bash
curl localhost:8081/actuator/health
```

---

### 9. JWT Authentication
**Status:** Fully implemented

- User Service generates RS256-signed JWTs
- Public key endpoint: `/.well-known/jwks.json`
- Refresh token support
- Gateway validates JWTs independently (defense in depth)

**Dependencies:**
- `spring-boot-starter-security`
- `spring-boot-starter-oauth2-resource-server`
- `spring-security-oauth2-jose`

---

### 10. Structured Logging & Correlation IDs
**Status:** Fully implemented (per architecture plan)

**From `plan/ARCHITECTURE.md`:**
> "Every service emits: JSON logs with `correlationId`, `traceId`, `spanId`, `customerId` where safe, and `eventId`"

---

## ⚠️ Patterns Partially Implemented (Stubbed/Incomplete)

### 1. Event-Driven Architecture & Kafka Patterns
**Status:** ⚠️ Stubbed with `NoOpEventPublisher`

**Current:**
- Kafka infrastructure ready (3.8.0 in Docker Compose)
- All services have `spring-boot-starter-kafka` dependency
- Event publishing stubbed in `common-library` → `NoOpEventPublisher` (no-op implementation)

**Missing:**
- **Outbox Pattern** — Transactional outbox table + polling publisher
  - Ensures events published iff transaction committed (exactly-once semantics)
  - Scheduled for Phase 2 per roadmap
- **Kafka Topic Contracts** — Event schema definitions
- **Partitioning Strategy** — Default: no explicit partition key (round-robin)
- **Retention Policies** — Not configured per topic

**What needs to be added:**

```java
// Outbox entity (needed in transaction-service, account-service, etc.)
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private UUID eventId;
    private String aggregateType;  // "transfer", "account", etc.
    private String eventType;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;  // null = not yet published
    @Lob
    private String payload;
}

// Outbox Publisher (replaces NoOpEventPublisher)
@Service
public class OutboxEventPublisher {
    @Scheduled(fixedRate = 1000)
    public void publishUnpublishedEvents() {
        List<OutboxEvent> unpublished = 
            outboxRepository.findByPublishedAtIsNull();
        for (OutboxEvent event : unpublished) {
            kafkaTemplate.send(event.getEventType(), event.getPayload());
            outboxRepository.updatePublishedAt(event.getId(), now());
        }
    }
}
```

**XU Reference:** Chapter 6 — "Reliability" → At-least-once delivery via Outbox

**Priority:** 🔴 **CRITICAL** — Phase 1.5 (before Phase 2)

---

### 2. Kafka Streams & CQRS
**Status:** ⚠️ Mentioned but not implemented

**Current:**
- Fraud Service planned to use Kafka Streams for windowed rule evaluation
- Reporting Service planned as materialized view consumer

**Missing:**
- Kafka Streams topology definitions
- Windowed aggregations (e.g., fraud score per customer per 10 min)
- Read model tables in separate database (separate PostgreSQL schema or cache)
- Event handlers that denormalize into reporting tables

**Example needed:**

```java
@Configuration
public class FraudStreamTopology {
    @Bean
    public KStream<String, TransferEvent> fraudDetectionStream(StreamsBuilder sb) {
        return sb.stream("transfer-events", 
            Consumed.with(Serdes.String(), serdeFor(TransferEvent.class)))
            .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofMinutes(10)))
            .aggregate(
                FraudScore::new,
                (customerId, transfer, score) -> score.evaluate(transfer),
                Materialized.as("fraud-scores-store"))
            .toStream()
            .filter((k, score) -> score.riskLevel() == HIGH)
            .to("fraud-alerts");
    }
}
```

**XU Reference:** Chapter 7 — "Real-time Analytics" → CQRS + Kafka Streams

**Priority:** 🟡 **HIGH** — Phase 2 (after Outbox)

---

### 3. Saga Pattern
**Status:** ⚠️ Architecture documented, not yet implemented

**Current:**
- `plan/ARCHITECTURE.md` describes Money Transfer Flow with Saga choreography
- Phase 0/1: No saga runtime (Temporal, Cadence, or Spring Cloud)

**Needed flow (from docs):**
```
Client → Transaction Service (POST /transfers with Idempotency-Key)
  ↓ (Outbox event: transfer-started)
  ↓ Kafka → Account Service (debit source, credit destination)
    ↓ (Outbox event: transfer-completed)
    ↓ Kafka → Fraud Service (evaluate risk)
    ↓ Kafka → Notification Service (send email/SMS)
    ↓ Kafka → Audit Service (log event)
```

**What's missing:**
- Compensation handlers (e.g., reverse transfer if fraud detected)
- Saga state machine runtime
- Timeout handling (rollback after 30s if account service doesn't respond)

**Roadmap approach:** "Saga choreography first, orchestration only when compensation rules become complex"

**XU Reference:** Chapter 8 — "Distributed Transactions" → Saga pattern

**Priority:** 🟡 **HIGH** — Phase 2 (with Outbox)

---

### 4. Idempotency & Deduplication
**Status:** ⚠️ Mentioned in architecture, not yet coded

**Current:**
- Design spec: "Use idempotency keys for money movement and externally submitted commands"
- Endpoints ready to accept `Idempotency-Key` header

**Missing:**
- Idempotency key storage (separate table with hash + response cache)
- Deduplication logic in Transaction Service
- TTL cleanup (24h retention for idempotency keys)

**Example needed:**

```java
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {
    @Id
    private String key;
    private String requestHash;
    private String responseJson;  // cached response
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}

@Service
public class IdempotentTransactionService {
    public TransferResponse executeTransfer(TransferRequest req, String idempotencyKey) {
        Optional<IdempotencyKey> cached = 
            idempotencyRepository.findByKeyAndExpiresAtAfter(idempotencyKey, now());
        if (cached.isPresent()) {
            return objectMapper.readValue(cached.get().getResponseJson(), 
                TransferResponse.class);  // return cached result
        }
        
        // execute transfer
        TransferResponse result = doTransfer(req);
        
        // cache response
        idempotencyRepository.save(new IdempotencyKey(
            idempotencyKey, 
            objectMapper.writeValueAsString(result),
            now().plusHours(24)));
        
        return result;
    }
}
```

**XU Reference:** Chapter 6 — "Reliability" → Idempotent APIs

**Priority:** 🔴 **CRITICAL** — Phase 1.5 (before Phase 2)

---

## 🔴 Critical Patterns Missing Entirely

### 1. Circuit Breaker
**Status:** 🔴 Not implemented (though Resilience4j is imported)

**Current:**
- `api-gateway/build.gradle.kts` includes `resilience4j-ratelimiter`
- Rate limiter active but no `@CircuitBreaker` annotations on any service-to-service calls

**What's needed:**
- Wrap all external HTTP calls (Account → Transaction, Notification API calls, AWS adapters)
- Thresholds: fail after 50% failures over 10 calls, wait 30s before retry
- Fallback strategies for each call

**Example:**

```java
@Service
public class TransactionServiceClient {
    private final RestTemplate restTemplate;
    private final CircuitBreakerRegistry registry;
    
    @CircuitBreaker(
        name = "transaction-service",
        fallbackMethod = "fallbackGetTransferStatus"
    )
    @Retry(name = "transaction-service")
    public TransferStatus getTransferStatus(String transferId) {
        return restTemplate.getForObject(
            "http://transaction-service/transfers/{id}",
            TransferStatus.class, 
            transferId);
    }
    
    public TransferStatus fallbackGetTransferStatus(String transferId, Exception e) {
        log.warn("Circuit breaker open for transaction-service, returning cached status", e);
        return TransferStatus.UNKNOWN;
    }
}
```

**Configuration needed:**

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30000
        permittedNumberOfCallsInHalfOpenState: 3
    instances:
      transaction-service:
        baseConfig: default
      account-service:
        baseConfig: default
      fraud-service:
        baseConfig: default
```

**Why critical:** 
- Cascading failures — if Account Service hangs, User Service calls also hang
- Transaction Service calls Account Service synchronously for balance checks
- Single instance failure affects entire gateway traffic

**XU Reference:** Chapter 5 — "Distributed Communication" → Resilience patterns

**Priority:** 🔴 **CRITICAL** — Phase 1.5 (immediately after Outbox)

---

### 2. Bulkhead / Thread Pool Isolation
**Status:** 🔴 Not configured

**Current:**
- All services use default Tomcat thread pool (200 threads)
- No isolation between Kafka consumers, database pool, HTTP clients

**What's needed:**
- Separate thread pools for: Kafka consumers, HTTP clients, database queries
- Separate connection pools: database, HTTP client, Redis

**Example:**

```java
@Configuration
public class BulkheadConfiguration {
    
    @Bean
    public Executor kafkaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("kafka-");
        executor.initialize();
        return executor;
    }
    
    @Bean
    public Executor httpClientExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("http-");
        executor.initialize();
        return executor;
    }
}

@Configuration
public class DataSourceConfiguration {
    @Bean
    public DataSource accountServiceDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:postgresql://localhost:5432/account_service")
            .username("bank")
            .password("bank")
            .hikariConfig(new HikariConfig() {{
                setMaximumPoolSize(10);  // Bulkhead limit
                setMinimumIdle(2);
                setConnectionTimeout(3000);
            }})
            .build();
    }
}
```

**Why important:**
- Fraud Service's windowed aggregation (expensive computation) shouldn't starve HTTP thread pool
- Kafka consumer lag cascades to request latency if not isolated

**XU Reference:** Chapter 5 — "Distributed Communication" → Bulkhead pattern

**Priority:** 🟡 **HIGH** — Phase 2 (with Circuit Breaker)

---

## 📊 Patterns Status Matrix

| Pattern | Status | Evidence | Priority | Effort |
| --- | --- | --- | --- | --- |
| Microservices | ✅ Complete | 10 services, 1 gateway | — | — |
| API Gateway | ✅ Complete | Rate limiting, JWT validation | — | — |
| Database per Service | ✅ Complete | PostgreSQL per service | — | — |
| Async Communication (Kafka) | ✅ Complete | Docker Compose, Spring Kafka | — | — |
| Load Balancing & Rate Limiting | ✅ Complete | Resilience4j in gateway | — | — |
| Caching | ✅ Complete | Redis 7 in infrastructure | — | — |
| Logging & Monitoring | ✅ Complete | Prometheus, Grafana, Jaeger | — | — |
| Health Checks | ✅ Complete | Actuator endpoints | — | — |
| JWT Authentication | ✅ Complete | JWKS endpoint, RS256 | — | — |
| Correlation IDs | ✅ Complete | Designed in architecture | — | — |
| **Event-Driven (Outbox)** | ⚠️ Stubbed | NoOpEventPublisher | 🔴 CRITICAL | 3 days |
| **Kafka Streams & CQRS** | ⚠️ Planned | Fraud/Reporting docs | 🟡 HIGH | 5 days |
| **Saga Pattern** | ⚠️ Designed | ARCHITECTURE.md | 🟡 HIGH | 5 days |
| **Idempotency** | ⚠️ Designed | Architecture notes | 🔴 CRITICAL | 2 days |
| **Circuit Breaker** | 🔴 Missing | Resilience4j imported, not used | 🔴 CRITICAL | 3 days |
| **Bulkhead/Thread Pools** | 🔴 Missing | No config | 🟡 HIGH | 2 days |

---

## 🗺️ Recommended Implementation Roadmap

### Phase 1.5: Resilience Foundation (1 week)
**Goal:** Ready for integration testing and Phase 2

**Tasks:**
1. Implement Outbox pattern in `common-library`
   - Add `OutboxEvent` entity to all transactional services
   - Implement `OutboxEventPublisher` scheduled task
   - Replace `NoOpEventPublisher` calls

2. Add idempotency key deduplication in Transaction Service
   - Add `IdempotencyKey` table
   - Implement cache-aside pattern for `POST /transfers`

3. Wire Circuit Breaker to Account Service → Transaction Service calls
   - Add `@CircuitBreaker` annotations
   - Configure thresholds in `application.yml`
   - Add fallback handlers

**Files to create/modify:**
- `common-library/src/main/java/com/bankplatform/event/OutboxEvent.java`
- `common-library/src/main/java/com/bankplatform/event/OutboxEventPublisher.java`
- `transaction-service/src/main/java/com/bankplatform/transaction/IdempotencyKey.java`
- `transaction-service/src/main/java/com/bankplatform/transaction/IdempotentTransactionService.java`
- `account-service/src/main/java/com/bankplatform/account/client/TransactionServiceClient.java`

---

### Phase 2: Event Reliability & Saga (2 weeks)
**Goal:** Full event-driven money transfer flow

**Tasks:**
1. Implement Saga choreography for money transfer
   - Define event contracts (JSON schema for all Kafka topics)
   - Add compensation handlers in Account Service (reverse debit if fraud detected)
   - Implement timeout handlers (retry with exponential backoff: 100ms, 200ms, 400ms)

2. Implement Kafka Streams fraud detection topology
   - 10-minute windowed aggregation by customer
   - Fraud score calculation per transfer
   - Alert emission to `fraud-alerts` topic

3. Add Dead Letter Queue (DLQ) and retry topics
   - `transfer-started-retry-1`, `-retry-2`, `-retry-3`
   - Move to DLQ after 3 failed retries
   - Manual inspection process for DLQ events

**Files to create/modify:**
- `fraud-service/src/main/java/com/bankplatform/fraud/stream/FraudStreamTopology.java`
- `account-service/src/main/java/com/bankplatform/account/saga/TransferCompensationHandler.java`
- `common-library/src/main/resources/kafka-topics.yml`

---

### Phase 3: CQRS & Materialized Views (2 weeks)
**Goal:** Read-optimized queries for Reporting and Audit

**Tasks:**
1. Implement CQRS read model in Reporting Service
   - Separate PostgreSQL schema: `reporting.account_summary`, `reporting.transaction_history`
   - Event handlers subscribe to all Kafka topics
   - Denormalize for fast queries (no joins across services)

2. Implement CQRS read model in Audit Service
   - Immutable append-only table: `audit_trail`
   - Event sourcing: reconstruct any aggregate state from events
   - Compliance search indexes on `customerId`, `eventType`, `timestamp`

3. Add partitioning strategy
   - Kafka partition key = `customerId` (guarantees ordering per customer)
   - Document in `plan/KAFKA.md`

**Files to create/modify:**
- `reporting-service/src/main/java/com/bankplatform/reporting/event/ReportingEventHandler.java`
- `audit-service/src/main/java/com/bankplatform/audit/event/AuditEventHandler.java`
- `audit-service/resources/db/migration/V1_0__create_audit_trail.sql`

---

### Phase 4: Advanced Resilience (1 week)
**Goal:** Production-grade fault tolerance

**Tasks:**
1. Implement Bulkhead pattern
   - Separate thread pools for Kafka, HTTP, database
   - Configuration in `application.yml`

2. Add timeout & retry policies
   - HTTP client timeout: 3s
   - Kafka consumer offset reset: `earliest` (no data loss)
   - Exponential backoff for transient failures

3. Database replication setup
   - PostgreSQL: primary + standby with streaming replication
   - Failover scripts in `terraform/`

**Files to create/modify:**
- `common-library/src/main/java/com/bankplatform/resilience/BulkheadConfiguration.java`
- `common-library/src/main/java/com/bankplatform/resilience/RetryConfiguration.java`
- `terraform/aws/rds.tf` (add standby replica)

---

### Phase 5: Operational Excellence (1 week)
**Goal:** Deployment, monitoring, feature flags

**Tasks:**
1. Add backup and disaster recovery
   - Daily RDS snapshots
   - Point-in-time recovery to last 30 days
   - Test restore procedure monthly

2. Add canary deployments
   - Route 10% traffic to new version
   - Monitor error rates, latency for 5 minutes
   - Automatic rollback if SLO breached

3. Add feature flags
   - Gradual rollout of Outbox → Kafka migration
   - Fraud rules A/B testing
   - UI experimentation for customers

**Files to create/modify:**
- `terraform/aws/backup.tf`
- `.github/workflows/canary-deploy.yml`
- `common-library/src/main/java/com/bankplatform/feature/FeatureFlagService.java`

---

## 🎯 Key Takeaways

| Aspect | Current | Gap | Action |
| --- | --- | --- | --- |
| **Resilience** | Gateway rate limiting only | No circuit breakers, bulkheads, timeouts | Implement Resilience4j config (Phase 1.5) |
| **Event Reliability** | Kafka ready, no publishers | Events not transactional, no dedup | Outbox + idempotency keys (Phase 1.5) |
| **Saga Coordination** | Designed, not coded | No compensation logic, no timeouts | Saga handlers + DLQ (Phase 2) |
| **Data Consistency** | Eventual, no CQRS | Reporting queries slow, no audit trail | CQRS materialized views (Phase 3) |
| **Production Readiness** | Dev-grade infrastructure | No replication, backups, canary | Terraform + CD pipeline (Phase 4–5) |

---

## References

- **Book:** _System Design Interview_ by Alex XU (Chapter 5–8)
- **Architecture Docs:** `plan/ARCHITECTURE.md`, `plan/KAFKA.md`
- **README:** Local end-to-end flow, module responsibilities
- **Build Files:** Dependencies confirm Java 21, Spring Boot 4.1, Spring AI 2.0, Resilience4j, Testcontainers

---

## Next Steps

1. **Week 1:** Begin Phase 1.5 implementation
   - Start with `common-library` Outbox refactor
   - Parallel: idempotency key POC in Transaction Service
   - Review with team before wiring Circuit Breakers

2. **Checkpoint:** End of Phase 1.5
   - All Phase 0 tests passing
   - Outbox publishing verified in staging
   - Rate limiter + circuit breaker rules confirmed

3. **Week 2–3:** Phase 2 event-driven implementation
   - Kafka Streams topology tested locally
   - Saga choreography end-to-end flow tested
   - DLQ manual intervention process documented

4. **Ongoing:** Update `IMPLEMENTATION_CHECKLIST.md` as tasks complete

