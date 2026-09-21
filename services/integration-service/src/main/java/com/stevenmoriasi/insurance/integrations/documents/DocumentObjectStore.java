package com.stevenmoriasi.insurance.integrations.documents;

import java.net.URI;
import java.time.Duration;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
public class DocumentObjectStore {

    private static final Duration SIGNATURE_DURATION = Duration.ofMinutes(10);

    private final S3Presigner presigner;
    private final String bucket;

    public DocumentObjectStore(S3Presigner presigner, ObjectStorageProperties properties) {
        this.presigner = presigner;
        this.bucket = properties.bucket();
    }

    public URI uploadUrl(String objectKey, String contentType) {
        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(contentType)
                        .build();
        return URI.create(
                presigner
                        .presignPutObject(
                                PutObjectPresignRequest.builder()
                                        .signatureDuration(SIGNATURE_DURATION)
                                        .putObjectRequest(request)
                                        .build())
                        .url()
                        .toString());
    }

    public URI downloadUrl(String objectKey) {
        return URI.create(
                presigner
                        .presignGetObject(
                                GetObjectPresignRequest.builder()
                                        .signatureDuration(SIGNATURE_DURATION)
                                        .getObjectRequest(
                                                GetObjectRequest.builder()
                                                        .bucket(bucket)
                                                        .key(objectKey)
                                                        .build())
                                        .build())
                        .url()
                        .toString());
    }
}
