package com.bankplatform.reporting.testfixtures;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton-container pattern: deliberately NOT annotated with {@code @Testcontainers}/
 * {@code @Container}. That JUnit5 extension manages start/stop per test *class*, so when multiple
 * integration test classes in the same module extend this base and share the one static field, one
 * class's afterAll stops the containers out from under the next class — surfacing as a "Connection
 * refused" once a module has two or more such test classes. Starting them once here and never
 * stopping them (Ryuk reaps them at JVM exit) avoids that.
 */
public abstract class PostgresAndS3TestcontainerBase {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("bank_platform_test")
                    .withUsername("bank")
                    .withPassword("bank");

    static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
                    .withServices(LocalStackContainer.Service.S3);

    static {
        try {
            POSTGRES.start();
            LOCALSTACK.start();
            LOCALSTACK.execInContainer("awslocal", "s3", "mb", "s3://bank-platform-statements");
        } catch (Exception e) {
            throw new RuntimeException("Failed to provision the test S3 bucket", e);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("bank-platform.reports.s3.region", () -> LOCALSTACK.getRegion());
        registry.add(
                "bank-platform.reports.s3.endpoint-override",
                () -> LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3).toString());
    }
}
