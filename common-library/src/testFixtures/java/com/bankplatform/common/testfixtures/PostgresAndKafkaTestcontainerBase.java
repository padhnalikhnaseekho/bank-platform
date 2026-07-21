package com.bankplatform.common.testfixtures;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Singleton-container pattern: deliberately NOT annotated with {@code @Testcontainers}/
 * {@code @Container}. That JUnit5 extension manages start/stop per test *class*, so when
 * multiple integration test classes in the same module extend this base and share the one
 * static field, one class's afterAll stops the containers out from under the next class —
 * surfacing as a "Connection refused" once a module has two or more such test classes.
 * Starting them once here and never stopping them (Ryuk reaps them at JVM exit) avoids that.
 */
public abstract class PostgresAndKafkaTestcontainerBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("bank_platform_test")
            .withUsername("bank")
            .withPassword("bank");

    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.8.0");

    static {
        POSTGRES.start();
        KAFKA.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}
