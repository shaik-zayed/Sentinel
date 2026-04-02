package org.sentinel.reportservice.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.url-expiry-minutes}")
    private int urlExpiryMinutes;

    /**
     * Checks whether a report already exists in MinIO.
     * Used to skip re-generation on duplicate requests.
     */
    public boolean exists(String objectPath) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectPath)
                            .build());
            return true;
        } catch (Exception e) {
            // statObject throws when object does not exist - that is the expected "not found" path
            return false;
        }
    }

    /**
     * Uploads a report file (as bytes) to MinIO.
     */
    public void upload(String objectPath, byte[] content, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectPath)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType(contentType)
                            .build());

            log.info("Uploaded report to MinIO: bucket={}, object={}, size={} bytes",
                    bucket, objectPath, content.length);

        } catch (Exception e) {
            log.error("Failed to upload to MinIO: object={}, error={}", objectPath, e.getMessage());
            throw new RuntimeException("Failed to store report in MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a pre-signed GET URL for a stored report.
     * The URL is valid for minio.url-expiry-minutes (default 60).
     * Direct download — bypasses all services, goes straight to MinIO.
     */
    public String generatePresignedUrl(String objectPath) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectPath)
                            .expiry(urlExpiryMinutes, TimeUnit.MINUTES)
                            .build());

            log.debug("Generated pre-signed URL for object={}, expiry={}min", objectPath, urlExpiryMinutes);
            return url;

        } catch (Exception e) {
            log.error("Failed to generate pre-signed URL: object={}, error={}", objectPath, e.getMessage());
            throw new RuntimeException("Failed to generate download URL: " + e.getMessage(), e);
        }
    }
}