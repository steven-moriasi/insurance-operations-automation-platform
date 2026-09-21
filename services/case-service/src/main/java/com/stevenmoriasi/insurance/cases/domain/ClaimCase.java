package com.stevenmoriasi.insurance.cases.domain;

import com.stevenmoriasi.insurance.cases.domain.CaseTypes.ClaimStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "claim_case")
public class ClaimCase {

    @Id private UUID id;
    private String claimReference;
    private UUID policyId;
    private UUID claimantPartyId;
    private LocalDate lossDate;
    private Instant reportedAt;

    @Enumerated(EnumType.STRING)
    private ClaimStatus status;

    private String ownerUsername;
    private Instant updatedAt;

    @Version private long version;

    protected ClaimCase() {}

    public ClaimCase(
            UUID id,
            String claimReference,
            UUID policyId,
            UUID claimantPartyId,
            LocalDate lossDate,
            Instant reportedAt,
            String ownerUsername) {
        this.id = id;
        this.claimReference = claimReference;
        this.policyId = policyId;
        this.claimantPartyId = claimantPartyId;
        this.lossDate = lossDate;
        this.reportedAt = reportedAt;
        this.status = ClaimStatus.OPEN;
        this.ownerUsername = ownerUsername;
        this.updatedAt = reportedAt;
    }

    public void moveTo(ClaimStatus status, Instant changedAt) {
        this.status = status;
        this.updatedAt = changedAt;
    }

    public void assignTo(String ownerUsername, Instant changedAt) {
        this.ownerUsername = ownerUsername;
        this.updatedAt = changedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getClaimReference() {
        return claimReference;
    }

    public UUID getPolicyId() {
        return policyId;
    }

    public UUID getClaimantPartyId() {
        return claimantPartyId;
    }

    public LocalDate getLossDate() {
        return lossDate;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
