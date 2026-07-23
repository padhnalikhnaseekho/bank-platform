package com.bankplatform.reporting.adapter.out.storage;

import com.bankplatform.reporting.application.port.ReportStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3ReportStorage implements ReportStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3ReportStorage(
            S3Client s3Client, @Value("${bank-platform.reports.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public String upload(String key, byte[] content, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(content));
        return "s3://" + bucket + "/" + key;
    }
}
