package com.stevenmoriasi.insurance.cases.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PolicyContextRepository extends JpaRepository<PolicyContext, UUID> {
    Optional<PolicyContext> findByPolicyNumber(String policyNumber);
}

interface PartyRepository extends JpaRepository<Party, UUID> {
    Optional<Party> findByExternalReference(String externalReference);
}

interface ClaimCaseRepository extends JpaRepository<ClaimCase, UUID> {
    Optional<ClaimCase> findByClaimReference(String claimReference);

    List<ClaimCase> findByOwnerUsernameOrderByUpdatedAtDesc(String ownerUsername);
}

interface CaseTaskRepository extends JpaRepository<CaseTask, UUID> {
    List<CaseTask> findByClaimIdOrderByCreatedAtAsc(UUID claimId);
}

interface EvidenceMetadataRepository extends JpaRepository<EvidenceMetadata, UUID> {
    List<EvidenceMetadata> findByClaimIdOrderBySubmittedAtAsc(UUID claimId);
}

interface AssessmentRepository extends JpaRepository<Assessment, UUID> {
    List<Assessment> findByClaimIdOrderBySubmittedAtAsc(UUID claimId);
}

interface DecisionRepository extends JpaRepository<Decision, UUID> {
    List<Decision> findByClaimIdOrderByRequestedAtAsc(UUID claimId);
}

interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    List<Settlement> findByClaimIdOrderByInstructedAtAsc(UUID claimId);
}

interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByAggregateIdOrderByOccurredAtAsc(UUID aggregateId);
}
