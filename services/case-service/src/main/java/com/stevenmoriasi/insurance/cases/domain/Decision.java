package com.stevenmoriasi.insurance.cases.domain;

import com.stevenmoriasi.insurance.cases.domain.CaseTypes.DecisionStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claim_decision")
public class Decision {

    @Id private UUID id;
    private UUID claimId;
    private UUID assessmentId;
    private String requestedBy;
    private BigDecimal amount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private DecisionStatus status;

    private String decidedBy;
    private String reason;
    private Instant requestedAt;
    private Instant decidedAt;

    protected Decision() {}

    public Decision(
            UUID id,
            UUID claimId,
            UUID assessmentId,
            String requestedBy,
            BigDecimal amount,
            String currency,
            String reason,
            Instant requestedAt) {
        this.id = id;
        this.claimId = claimId;
        this.assessmentId = assessmentId;
        this.requestedBy = requestedBy;
        this.amount = amount;
        this.currency = currency;
        this.status = DecisionStatus.PENDING_CHECKER;
        this.reason = reason;
        this.requestedAt = requestedAt;
    }

    public void approve(String decidedBy, Instant decidedAt) {
        this.status = DecisionStatus.APPROVED;
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
    }

    public void reject(String decidedBy, String reason, Instant decidedAt) {
        this.status = DecisionStatus.REJECTED;
        this.decidedBy = decidedBy;
        this.reason = reason;
        this.decidedAt = decidedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClaimId() {
        return claimId;
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public DecisionStatus getStatus() {
        return status;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public String getReason() {
        return reason;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
