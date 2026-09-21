package com.stevenmoriasi.insurance.integrations.documents;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_object")
public class DocumentObject {

    @Id private UUID id;
    private String idempotencyKey;
    private String claimReference;
    private String fileName;
    private String objectKey;
    private String contentType;
    private String expectedSha256;
    private long maximumBytes;
    private UUID scanWorkItemId;
    private Long observedBytes;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    private String rejectionReason;
    private Instant createdAt;
    private Instant scannedAt;

    @Version private long version;

    protected DocumentObject() {}

    public DocumentObject(
            UUID id,
            String idempotencyKey,
            String claimReference,
            String fileName,
            String objectKey,
            String contentType,
            String expectedSha256,
            long maximumBytes,
            Instant createdAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.claimReference = claimReference;
        this.fileName = fileName;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.expectedSha256 = expectedSha256;
        this.maximumBytes = maximumBytes;
        this.status = DocumentStatus.PENDING_SCAN;
        this.createdAt = createdAt;
    }

    public void assignScanWorkItem(UUID scanWorkItemId) {
        this.scanWorkItemId = scanWorkItemId;
    }

    public void recordScan(String observedSha256, long observedBytes, boolean clean, Instant now) {
        this.observedBytes = observedBytes;
        this.scannedAt = now;
        if (!expectedSha256.equalsIgnoreCase(observedSha256)) {
            reject("Uploaded object digest did not match the registration");
        } else if (observedBytes > maximumBytes) {
            reject("Uploaded object exceeded the registered size limit");
        } else if (!clean) {
            reject("Document scanner rejected the uploaded object");
        } else {
            status = DocumentStatus.AVAILABLE;
            rejectionReason = null;
        }
    }

    private void reject(String reason) {
        status = DocumentStatus.REJECTED;
        rejectionReason = reason;
    }

    public UUID getId() {
        return id;
    }

    public String getClaimReference() {
        return claimReference;
    }

    public String getFileName() {
        return fileName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExpectedSha256() {
        return expectedSha256;
    }

    public long getMaximumBytes() {
        return maximumBytes;
    }

    public UUID getScanWorkItemId() {
        return scanWorkItemId;
    }

    public Long getObservedBytes() {
        return observedBytes;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getScannedAt() {
        return scannedAt;
    }

    public enum DocumentStatus {
        PENDING_SCAN,
        AVAILABLE,
        REJECTED
    }
}
