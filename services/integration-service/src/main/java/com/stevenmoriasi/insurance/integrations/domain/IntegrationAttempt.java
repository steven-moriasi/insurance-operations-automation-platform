package com.stevenmoriasi.insurance.integrations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_attempt")
public class IntegrationAttempt {

    @Id private UUID id;
    private String idempotencyKey;
    private String operationType;
    private String subjectReference;
    private String requestHash;

    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(columnDefinition = "text")
    private String responseJson;

    private Instant createdAt;
    private Instant updatedAt;

    @Version private long version;

    protected IntegrationAttempt() {}

    public IntegrationAttempt(
            UUID id,
            String idempotencyKey,
            String operationType,
            String subjectReference,
            String requestHash,
            Instant now) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.operationType = operationType;
        this.subjectReference = subjectReference;
        this.requestHash = requestHash;
        this.status = IntegrationStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void complete(String responseJson, Instant now) {
        this.status = IntegrationStatus.COMPLETED;
        this.responseJson = responseJson;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public IntegrationStatus getStatus() {
        return status;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public enum IntegrationStatus {
        PENDING,
        COMPLETED,
        FAILED
    }
}
