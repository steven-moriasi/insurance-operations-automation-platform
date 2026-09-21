package com.stevenmoriasi.insurance.cases.domain;

import com.stevenmoriasi.insurance.cases.domain.CaseTypes.EvidenceStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_metadata")
public class EvidenceMetadata {

    @Id private UUID id;
    private UUID claimId;
    private String evidenceType;
    private String objectKey;
    private String mediaType;
    private long sizeBytes;
    private String sha256;

    @Enumerated(EnumType.STRING)
    private EvidenceStatus status;

    private Instant submittedAt;

    protected EvidenceMetadata() {}

    public EvidenceMetadata(
            UUID id,
            UUID claimId,
            String evidenceType,
            String objectKey,
            String mediaType,
            long sizeBytes,
            String sha256,
            Instant submittedAt) {
        this.id = id;
        this.claimId = claimId;
        this.evidenceType = evidenceType;
        this.objectKey = objectKey;
        this.mediaType = mediaType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.status = EvidenceStatus.RECEIVED;
        this.submittedAt = submittedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClaimId() {
        return claimId;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getMediaType() {
        return mediaType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public EvidenceStatus getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
