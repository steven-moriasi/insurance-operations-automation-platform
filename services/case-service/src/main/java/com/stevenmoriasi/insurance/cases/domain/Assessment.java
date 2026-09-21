package com.stevenmoriasi.insurance.cases.domain;

import com.stevenmoriasi.insurance.cases.domain.CaseTypes.AssessmentOutcome;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessment")
public class Assessment {

    @Id private UUID id;
    private UUID claimId;
    private String assessorUsername;
    private BigDecimal recommendedAmount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private AssessmentOutcome outcome;

    private String rationale;
    private Instant submittedAt;

    protected Assessment() {}

    public Assessment(
            UUID id,
            UUID claimId,
            String assessorUsername,
            BigDecimal recommendedAmount,
            String currency,
            AssessmentOutcome outcome,
            String rationale,
            Instant submittedAt) {
        this.id = id;
        this.claimId = claimId;
        this.assessorUsername = assessorUsername;
        this.recommendedAmount = recommendedAmount;
        this.currency = currency;
        this.outcome = outcome;
        this.rationale = rationale;
        this.submittedAt = submittedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClaimId() {
        return claimId;
    }

    public String getAssessorUsername() {
        return assessorUsername;
    }

    public BigDecimal getRecommendedAmount() {
        return recommendedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public AssessmentOutcome getOutcome() {
        return outcome;
    }

    public String getRationale() {
        return rationale;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
