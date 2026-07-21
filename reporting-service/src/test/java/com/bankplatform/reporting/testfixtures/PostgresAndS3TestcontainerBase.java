package com.bankplatform.reporting.testfixtures;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class PostgresAndS3TestcontainerBase {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("bank_platform_test")
            .withUsername("bank")
            .withPassword("bank");

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3")).withServices(LocalStackContainer.Service.S3);

    static {
        try {
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
        registry.add("bank-platform.reports.s3.endpoint-override",
                () -> LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3).toString());
    }
}
