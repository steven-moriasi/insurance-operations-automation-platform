package com.stevenmoriasi.insurance.integrations.payments;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_instruction")
public class PaymentInstruction {

    @Id private UUID id;
    private String idempotencyKey;
    private String claimReference;
    private BigDecimal amount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String providerReference;
    private String callbackHash;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant confirmedAt;

    @Version private long version;

    protected PaymentInstruction() {}

    public PaymentInstruction(
            UUID id,
            String idempotencyKey,
            String claimReference,
            BigDecimal amount,
            String currency,
            String providerReference,
            Instant now) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.claimReference = claimReference;
        this.amount = amount;
        this.currency = currency;
        this.providerReference = providerReference;
        this.status = PaymentStatus.INSTRUCTED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void confirm(String callbackHash, Instant now) {
        this.status = PaymentStatus.CONFIRMED;
        this.callbackHash = callbackHash;
        this.confirmedAt = now;
        this.updatedAt = now;
    }

    public void fail(String callbackHash, String reason, Instant now) {
        this.status = PaymentStatus.FAILED;
        this.callbackHash = callbackHash;
        this.failureReason = reason;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getClaimReference() {
        return claimReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public String getCallbackHash() {
        return callbackHash;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public enum PaymentStatus {
        INSTRUCTED,
        CONFIRMED,
        FAILED
    }
}
