package com.stevenmoriasi.insurance.integrations.documents;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("insurance.object-storage")
public record ObjectStorageProperties(
        String endpoint, String region, String bucket, String accessKey, String secretKey) {}
