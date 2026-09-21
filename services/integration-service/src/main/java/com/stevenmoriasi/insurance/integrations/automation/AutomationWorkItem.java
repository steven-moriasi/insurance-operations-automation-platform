package com.stevenmoriasi.insurance.integrations.automation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "automation_work_item")
public class AutomationWorkItem {

    @Id private UUID id;
    private String idempotencyKey;
    private String workType;
    private String businessReference;

    @Column(columnDefinition = "text")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    private WorkStatus status;

    private Instant availableAt;
    private String leaseOwner;
    private UUID leaseToken;
    private Instant leaseExpiresAt;
    private int attempts;
    private int maximumAttempts;

    @Column(columnDefinition = "text")
    private String resultJson;

    private String lastError;
    private Instant createdAt;
    private Instant completedAt;

    @Version private long version;

    protected AutomationWorkItem() {}

    public AutomationWorkItem(
            UUID id,
            String idempotencyKey,
            String workType,
            String businessReference,
            String payloadJson,
            int maximumAttempts,
            Instant now) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.workType = workType;
        this.businessReference = businessReference;
        this.payloadJson = payloadJson;
        this.status = WorkStatus.READY;
        this.availableAt = now;
        this.attempts = 0;
        this.maximumAttempts = maximumAttempts;
        this.createdAt = now;
    }

    public UUID lease(String workerId, Duration leaseDuration, Instant now) {
        this.status = WorkStatus.LEASED;
        this.leaseOwner = workerId;
        this.leaseToken = UUID.randomUUID();
        this.leaseExpiresAt = now.plus(leaseDuration);
        this.attempts++;
        return leaseToken;
    }

    public void complete(UUID suppliedToken, String resultJson, Instant now) {
        requireLease(suppliedToken, now);
        this.status = WorkStatus.COMPLETED;
        this.resultJson = resultJson;
        this.completedAt = now;
        clearLease();
    }

    public void fail(UUID suppliedToken, String error, Instant now) {
        requireLease(suppliedToken, now);
        this.lastError = error;
        if (attempts >= maximumAttempts) {
            this.status = WorkStatus.DEAD_LETTER;
        } else {
            this.status = WorkStatus.READY;
            long delaySeconds = Math.min(30L * (1L << Math.min(attempts - 1, 4)), 600L);
            this.availableAt = now.plusSeconds(delaySeconds);
        }
        clearLease();
    }

    public void recoverExpiredLease(Instant now) {
        if (status == WorkStatus.LEASED && leaseExpiresAt.isBefore(now)) {
            status = WorkStatus.READY;
            availableAt = now;
            lastError = "Worker lease expired before completion";
            clearLease();
        }
    }

    private void requireLease(UUID suppliedToken, Instant now) {
        if (status != WorkStatus.LEASED
                || leaseToken == null
                || !leaseToken.equals(suppliedToken)
                || !leaseExpiresAt.isAfter(now)) {
            throw new InvalidWorkLeaseException();
        }
    }

    private void clearLease() {
        leaseOwner = null;
        leaseToken = null;
        leaseExpiresAt = null;
    }

    public UUID getId() {
        return id;
    }

    public String getWorkType() {
        return workType;
    }

    public String getBusinessReference() {
        return businessReference;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public WorkStatus getStatus() {
        return status;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public UUID getLeaseToken() {
        return leaseToken;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaximumAttempts() {
        return maximumAttempts;
    }

    public enum WorkStatus {
        READY,
        LEASED,
        COMPLETED,
        DEAD_LETTER
    }
}
