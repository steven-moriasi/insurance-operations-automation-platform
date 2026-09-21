package com.stevenmoriasi.insurance.cases.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stevenmoriasi.insurance.cases.domain.CaseTypes.AssessmentOutcome;
import com.stevenmoriasi.insurance.cases.domain.CaseTypes.ClaimStatus;
import com.stevenmoriasi.insurance.cases.domain.CaseTypes.DecisionStatus;
import com.stevenmoriasi.insurance.cases.domain.CaseTypes.PartyType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseManagementService {

    private final PolicyContextRepository policies;
    private final PartyRepository parties;
    private final ClaimCaseRepository claims;
    private final CaseTaskRepository tasks;
    private final EvidenceMetadataRepository evidence;
    private final AssessmentRepository assessments;
    private final DecisionRepository decisions;
    private final SettlementRepository settlements;
    private final AuditEventRepository auditEvents;
    private final CaseAccessPolicy accessPolicy;
    private final ApprovalAuthorityPolicy authorityPolicy;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public CaseManagementService(
            PolicyContextRepository policies,
            PartyRepository parties,
            ClaimCaseRepository claims,
            CaseTaskRepository tasks,
            EvidenceMetadataRepository evidence,
            AssessmentRepository assessments,
            DecisionRepository decisions,
            SettlementRepository settlements,
            AuditEventRepository auditEvents,
            CaseAccessPolicy accessPolicy,
            ApprovalAuthorityPolicy authorityPolicy,
            ObjectMapper objectMapper) {
        this.policies = policies;
        this.parties = parties;
        this.claims = claims;
        this.tasks = tasks;
        this.evidence = evidence;
        this.assessments = assessments;
        this.decisions = decisions;
        this.settlements = settlements;
        this.auditEvents = auditEvents;
        this.accessPolicy = accessPolicy;
        this.authorityPolicy = authorityPolicy;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public ClaimCase reportClaim(ReportClaim command, CaseActor actor) {
        accessPolicy.requireAnyRole(actor, "CLAIMS_OFFICER", "CLAIMS_SUPERVISOR", "PLATFORM_ADMIN");
        if (claims.findByClaimReference(command.claimReference()).isPresent()) {
            throw conflict("Claim reference already exists");
        }

        PolicyContext policy =
                policies.findByPolicyNumber(command.policyNumber())
                        .orElseGet(
                                () ->
                                        policies.save(
                                                new PolicyContext(
                                                        UUID.randomUUID(),
                                                        command.policyNumber(),
                                                        command.productCode(),
                                                        command.policyStatus(),
                                                        command.coverStartDate(),
                                                        command.coverEndDate(),
                                                        command.currency())));
        Party claimant =
                parties.findByExternalReference(command.claimantExternalReference())
                        .orElseGet(
                                () ->
                                        parties.save(
                                                new Party(
                                                        UUID.randomUUID(),
                                                        PartyType.CLAIMANT,
                                                        command.claimantExternalReference(),
                                                        command.claimantName(),
                                                        command.claimantPhoneNumber(),
                                                        command.claimantEmailAddress())));

        Instant now = clock.instant();
        ClaimCase claim =
                claims.save(
                        new ClaimCase(
                                UUID.randomUUID(),
                                command.claimReference(),
                                policy.getId(),
                                claimant.getId(),
                                command.lossDate(),
                                now,
                                actor.username()));
        audit(
                claim.getId(),
                "CLAIM_REPORTED",
                actor,
                Map.of(
                        "claimReference", claim.getClaimReference(),
                        "policyNumber", policy.getPolicyNumber()));
        return claim;
    }

    @Transactional(readOnly = true)
    public CaseSnapshot getCase(String claimReference, CaseActor actor) {
        ClaimCase claim = requireClaim(claimReference);
        accessPolicy.requireCaseAccess(claim, actor);
        return new CaseSnapshot(
                claim,
                requirePolicy(claim.getPolicyId()),
                requireParty(claim.getClaimantPartyId()),
                tasks.findByClaimIdOrderByCreatedAtAsc(claim.getId()),
                evidence.findByClaimIdOrderBySubmittedAtAsc(claim.getId()),
                assessments.findByClaimIdOrderBySubmittedAtAsc(claim.getId()),
                decisions.findByClaimIdOrderByRequestedAtAsc(claim.getId()),
                settlements.findByClaimIdOrderByCreatedAtAsc(claim.getId()),
                auditEvents.findByAggregateIdOrderByOccurredAtAsc(claim.getId()));
    }

    @Transactional(readOnly = true)
    public List<ClaimCase> getOwnedCases(CaseActor actor) {
        return claims.findByOwnerUsernameOrderByUpdatedAtDesc(actor.username());
    }

    @Transactional
    public CaseTask createTask(String claimReference, CreateTask command, CaseActor actor) {
        ClaimCase claim = requireClaim(claimReference);
        accessPolicy.requireCaseAccess(claim, actor);
        accessPolicy.requireAnyRole(actor, "CLAIMS_OFFICER", "CLAIMS_SUPERVISOR", "PLATFORM_ADMIN");
        Instant now = clock.instant();
        if (!command.dueAt().isAfter(now)) {
            throw invalid("Task due time must be in the future");
        }
        CaseTask task =
                tasks.save(
                        new CaseTask(
                                UUID.randomUUID(),
                                claim.getId(),
                                command.taskType(),
                                command.assignee(),
                                command.dueAt(),
                                now));
        audit(
                claim.getId(),
                "TASK_CREATED",
                actor,
                Map.of("taskId", task.getId(), "taskType", task.getTaskType()));
        return task;
    }

    @Transactional
    public CaseTask completeTask(String claimReference, UUID taskId, CaseActor actor) {
        ClaimCase claim = requireClaim(claimReference);
        accessPolicy.requireCaseAccess(claim, actor);
        CaseTask task = requireTask(taskId);
        requireBelongsToClaim(task.getClaimId(), claim);
        if (task.getAssignee() != null
                && !task.getAssignee().equals(actor.username())
                && !actor.hasAnyRole("CLAIMS_SUPERVISOR", "PLATFORM_ADMIN")) {
            throw forbidden("Only the assignee or a supervisor can complete this task");
        }
        task.complete(clock.instant());
        audit(claim.getId(), "TASK_COMPLETED", actor, Map.of("taskId", task.getId()));
        return task;
    }

    @Transactional
    public EvidenceMetadata recordEvidence(
            String claimReference, RecordEvidence command, CaseActor actor) {
        ClaimCase claim = requireClaim(claimReference);
        accessPolicy.requireCaseAccess(claim, actor);
        accessPolicy.requireAnyRole(
                actor, "CLAIMS_OFFICER", "CLAIMS_ASSESSOR", "CLAIMS_SUPERVISOR", "PLATFORM_ADMIN");
        if (command.sizeBytes() <= 0) {
            throw invalid("Evidence size must be positive");
        }
        EvidenceMetadata metadata =
                evidence.save(
                        new EvidenceMetadata(
                                UUID.randomUUID(),
                                claim.getId(),
                                command.evidenceType(),
                                command.objectKey(),
                                command.mediaType(),
                                command.sizeBytes(),
                                command.sha256(),
                                clock.instant()));
        claim.moveTo(ClaimStatus.AWAITING_EVIDENCE, clock.instant());
        audit(
                claim.getId(),
                "EVIDENCE_RECORDED",
                actor,
                Map.of("evidenceId", metadata.getId(), "evidenceType", metadata.getEvidenceType()));
        return metadata;
    }

    @Transactional
    public Assessment submitAssessment(
            String claimReference, SubmitAssessment command, CaseActor actor) {
        ClaimCase claim = requireClaim(claimReference);
        accessPolicy.requireCaseAccess(claim, actor);
        accessPolicy.requireAnyRole(
                actor, "CLAIMS_ASSESSOR", "CLAIMS_SUPERVISOR", "PLATFORM_ADMIN");
        requirePositive(command.recommendedAmount(), "Recommended amount");
        Assessment assessment =
                assessments.save(
                        new Assessment(
                                UUID.randomUUID(),
                                claim.getId(),
                                actor.username(),
                                command.recommendedAmount(),
                                command.currency(),
                                command.outcome(),
                                command.rationale(),
                                clock.instant()));
        ClaimStatus status =
                command.outcome() == AssessmentOutcome.RECOMMEND_APPROVAL
                        ? ClaimStatus.AWAITING_APPROVAL
                        : ClaimStatus.UNDER_ASSESSMENT;
        claim.moveTo(status, clock.instant());
        audit(
                claim.getId(),
                "ASSESSMENT_SUBMITTED",
                actor,
                Map.of("assessmentId", assessment.getId(), "outcome", assessment.getOutcome()));
        return assessment;
    }

    @Transactional
    public Decision requestDecision(
            String claimReference, RequestDecision command, CaseActor actor) {
        ClaimCase claim = requireClaim(claimReference);
        accessPolicy.requireCaseAccess(claim, actor);
        accessPolicy.requireAnyRole(actor, "CLAIMS_OFFICER", "CLAIMS_SUPERVISOR", "PLATFORM_ADMIN");
        Assessment assessment = requireAssessment(command.assessmentId());
        requireBelongsToClaim(assessment.getClaimId(), claim);
        requirePositive(command.amount(), "Decision amount");
        if (!assessment.getCurrency().equals(command.currency())) {
            throw invalid("Decision currency must match the assessment");
        }
        if (command.amount().compareTo(assessment.getRecommendedAmount()) > 0) {
            throw invalid("Decision amount cannot exceed the assessed recommendation");
        }
        Decision decision =
                decisions.save(
                        new Decision(
                                UUID.randomUUID(),
                                claim.getId(),
                                assessment.getId(),
                                actor.username(),
                                command.amount(),
                                command.currency(),
                                command.reason(),
                                clock.instant()));
        claim.moveTo(ClaimStatus.AWAITING_APPROVAL, clock.instant());
        audit(
                claim.getId(),
                "DECISION_REQUESTED",
                actor,
                Map.of("decisionId", decision.getId(), "amount", decision.getAmount()));
        return decision;
    }

    @Transactional
    public Decision approveDecision(String claimReference, UUID decisionId, CaseActor actor) {
        ClaimCase claim = requireClaim(claimReference);
        accessPolicy.requireCaseAccess(claim, actor);
        Decision decision = requireDecision(decisionId);
        requireBelongsToClaim(decision.getClaimId(), claim);
        if (decision.getStatus() != DecisionStatus.PENDING_CHECKER) {
            throw conflict("Decision is not awaiting checker approval");
        }
        if (decision.getRequestedBy().equals(actor.username())) {
            throw forbidden("The decision requester cannot approve the same decision");
        }
        authorityPolicy.requireAuthority(actor, decision.getAmount());
        decision.approve(actor.username(), clock.instant());
        claim.moveTo(ClaimStatus.APPROVED, clock.instant());
        audit(
                claim.getId(),
                "DECISION_APPROVED",
                actor,
                Map.of("decisionId", decision.getId(), "amount", decision.getAmount()));
        return decision;
    }

    @Transactional
    public Settlement createSettlement(String claimReference, UUID decisionId, CaseActor actor) {
        ClaimCase claim = requireClaim(claimReference);
        accessPolicy.requireCaseAccess(claim, actor);
        accessPolicy.requireAnyRole(actor, "FINANCE_OPERATOR", "PLATFORM_ADMIN");
        Decision decision = requireDecision(decisionId);
        requireBelongsToClaim(decision.getClaimId(), claim);
        if (decision.getStatus() != DecisionStatus.APPROVED) {
            throw conflict("Settlement requires an approved decision");
        }
        Settlement settlement =
                settlements.save(
                        new Settlement(
                                UUID.randomUUID(),
                                claim.getId(),
                                decision.getId(),
                                decision.getAmount(),
                                decision.getCurrency(),
                                clock.instant()));
        audit(
                claim.getId(),
                "SETTLEMENT_CREATED",
                actor,
                Map.of("settlementId", settlement.getId(), "amount", settlement.getAmount()));
        return settlement;
    }

    @Transactional
    public Settlement markSettlementInstructed(
            String claimReference, UUID settlementId, String externalReference, CaseActor actor) {
        ClaimCase claim = requireClaim(claimReference);
        accessPolicy.requireCaseAccess(claim, actor);
        accessPolicy.requireAnyRole(actor, "FINANCE_OPERATOR", "PLATFORM_ADMIN");
        Settlement settlement = requireSettlement(settlementId);
        requireBelongsToClaim(settlement.getClaimId(), claim);
        settlement.markInstructed(externalReference, clock.instant());
        claim.moveTo(ClaimStatus.SETTLEMENT_INSTRUCTED, clock.instant());
        audit(
                claim.getId(),
                "SETTLEMENT_INSTRUCTED",
                actor,
                Map.of("settlementId", settlement.getId(), "externalReference", externalReference));
        return settlement;
    }

    @Transactional
    public Settlement reconcileSettlement(
            String claimReference, UUID settlementId, CaseActor actor) {
        ClaimCase claim = requireClaim(claimReference);
        accessPolicy.requireCaseAccess(claim, actor);
        accessPolicy.requireAnyRole(actor, "FINANCE_OPERATOR", "PLATFORM_ADMIN");
        Settlement settlement = requireSettlement(settlementId);
        requireBelongsToClaim(settlement.getClaimId(), claim);
        settlement.reconcile(clock.instant());
        claim.moveTo(ClaimStatus.SETTLED, clock.instant());
        audit(
                claim.getId(),
                "SETTLEMENT_RECONCILED",
                actor,
                Map.of("settlementId", settlement.getId()));
        return settlement;
    }

    private ClaimCase requireClaim(String claimReference) {
        return claims.findByClaimReference(claimReference).orElseThrow(() -> notFound("Claim"));
    }

    private PolicyContext requirePolicy(UUID id) {
        return policies.findById(id).orElseThrow(() -> notFound("Policy"));
    }

    private Party requireParty(UUID id) {
        return parties.findById(id).orElseThrow(() -> notFound("Party"));
    }

    private CaseTask requireTask(UUID id) {
        return tasks.findById(id).orElseThrow(() -> notFound("Task"));
    }

    private Assessment requireAssessment(UUID id) {
        return assessments.findById(id).orElseThrow(() -> notFound("Assessment"));
    }

    private Decision requireDecision(UUID id) {
        return decisions.findById(id).orElseThrow(() -> notFound("Decision"));
    }

    private Settlement requireSettlement(UUID id) {
        return settlements.findById(id).orElseThrow(() -> notFound("Settlement"));
    }

    private static void requireBelongsToClaim(UUID actualClaimId, ClaimCase claim) {
        if (!actualClaimId.equals(claim.getId())) {
            throw notFound("Case record");
        }
    }

    private static void requirePositive(BigDecimal amount, String label) {
        if (amount == null || amount.signum() <= 0) {
            throw invalid(label + " must be positive");
        }
    }

    private void audit(UUID claimId, String eventType, CaseActor actor, Map<String, ?> data) {
        auditEvents.save(
                new AuditEvent(
                        UUID.randomUUID(),
                        "CLAIM",
                        claimId,
                        eventType,
                        actor.username(),
                        clock.instant(),
                        toJson(data)));
    }

    private String toJson(Map<String, ?> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize audit event", exception);
        }
    }

    private static CaseOperationException notFound(String resource) {
        return new CaseOperationException(
                CaseOperationException.Reason.NOT_FOUND, resource + " was not found");
    }

    private static CaseOperationException forbidden(String message) {
        return new CaseOperationException(CaseOperationException.Reason.FORBIDDEN, message);
    }

    private static CaseOperationException conflict(String message) {
        return new CaseOperationException(CaseOperationException.Reason.CONFLICT, message);
    }

    private static CaseOperationException invalid(String message) {
        return new CaseOperationException(CaseOperationException.Reason.INVALID_REQUEST, message);
    }

    public record ReportClaim(
            String claimReference,
            String policyNumber,
            String productCode,
            String policyStatus,
            LocalDate coverStartDate,
            LocalDate coverEndDate,
            String currency,
            String claimantExternalReference,
            String claimantName,
            String claimantPhoneNumber,
            String claimantEmailAddress,
            LocalDate lossDate) {}

    public record CreateTask(String taskType, String assignee, Instant dueAt) {}

    public record RecordEvidence(
            String evidenceType,
            String objectKey,
            String mediaType,
            long sizeBytes,
            String sha256) {}

    public record SubmitAssessment(
            BigDecimal recommendedAmount,
            String currency,
            AssessmentOutcome outcome,
            String rationale) {}

    public record RequestDecision(
            UUID assessmentId, BigDecimal amount, String currency, String reason) {}

    public record CaseSnapshot(
            ClaimCase claim,
            PolicyContext policy,
            Party claimant,
            List<CaseTask> tasks,
            List<EvidenceMetadata> evidence,
            List<Assessment> assessments,
            List<Decision> decisions,
            List<Settlement> settlements,
            List<AuditEvent> auditEvents) {}
}
