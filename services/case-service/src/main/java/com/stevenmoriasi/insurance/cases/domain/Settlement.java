package com.stevenmoriasi.insurance.cases.domain;

import com.stevenmoriasi.insurance.cases.domain.CaseTypes.SettlementStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlement")
public class Settlement {

    @Id private UUID id;
    private UUID claimId;
    private UUID decisionId;
    private BigDecimal amount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private SettlementStatus status;

    private String externalReference;
    private Instant createdAt;
    private Instant instructedAt;
    private Instant reconciledAt;

    protected Settlement() {}

    public Settlement(
            UUID id,
            UUID claimId,
            UUID decisionId,
            BigDecimal amount,
            String currency,
            Instant createdAt) {
        this.id = id;
        this.claimId = claimId;
        this.decisionId = decisionId;
        this.amount = amount;
        this.currency = currency;
        this.status = SettlementStatus.PENDING;
        this.createdAt = createdAt;
    }

    public void markInstructed(String externalReference, Instant instructedAt) {
        this.status = SettlementStatus.INSTRUCTED;
        this.externalReference = externalReference;
        this.instructedAt = instructedAt;
    }

    public void reconcile(Instant reconciledAt) {
        this.status = SettlementStatus.RECONCILED;
        this.reconciledAt = reconciledAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClaimId() {
        return claimId;
    }

    public UUID getDecisionId() {
        return decisionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getInstructedAt() {
        return instructedAt;
    }

    public Instant getReconciledAt() {
        return reconciledAt;
    }
}
