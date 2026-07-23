package com.bankplatform.common.testfixtures;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Singleton-container pattern: deliberately NOT annotated with {@code @Testcontainers}/
 * {@code @Container}. That JUnit5 extension manages start/stop per test *class*, so when multiple
 * integration test classes in the same module extend this base and share the one static field, one
 * class's afterAll stops the container out from under the next class — surfacing as a "Connection
 * refused" once a module has two or more such test classes. Starting it once here and never
 * stopping it (Ryuk reaps it at JVM exit) avoids that.
 */
public abstract class PostgresTestcontainerBase {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("bank_platform_test")
                    .withUsername("bank")
                    .withPassword("bank");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
