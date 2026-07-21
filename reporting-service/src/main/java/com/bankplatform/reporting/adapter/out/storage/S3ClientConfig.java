package com.bankplatform.reporting.adapter.out.storage;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
public class S3ClientConfig {

    @Bean
    public S3Client s3Client(@Value("${bank-platform.reports.s3.region}") String region,
            @Value("${bank-platform.reports.s3.endpoint-override:}") String endpointOverride) {
        S3ClientBuilder builder = S3Client.builder().region(Region.of(region));
        if (endpointOverride != null && !endpointOverride.isBlank()) {
            // LocalStack: path-style + dummy credentials, matching plan/AWS.md's local-dev mode.
            builder.endpointOverride(URI.create(endpointOverride)).forcePathStyle(true)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }
}
