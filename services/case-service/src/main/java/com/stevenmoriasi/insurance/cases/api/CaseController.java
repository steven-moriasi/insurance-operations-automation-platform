package com.stevenmoriasi.insurance.cases.api;

import com.stevenmoriasi.insurance.cases.domain.Assessment;
import com.stevenmoriasi.insurance.cases.domain.AuditEvent;
import com.stevenmoriasi.insurance.cases.domain.CaseActor;
import com.stevenmoriasi.insurance.cases.domain.CaseManagementService;
import com.stevenmoriasi.insurance.cases.domain.CaseManagementService.CaseSnapshot;
import com.stevenmoriasi.insurance.cases.domain.CaseManagementService.CreateTask;
import com.stevenmoriasi.insurance.cases.domain.CaseManagementService.RecordEvidence;
import com.stevenmoriasi.insurance.cases.domain.CaseManagementService.ReportClaim;
import com.stevenmoriasi.insurance.cases.domain.CaseManagementService.RequestDecision;
import com.stevenmoriasi.insurance.cases.domain.CaseManagementService.SubmitAssessment;
import com.stevenmoriasi.insurance.cases.domain.CaseTask;
import com.stevenmoriasi.insurance.cases.domain.CaseTypes.AssessmentOutcome;
import com.stevenmoriasi.insurance.cases.domain.ClaimCase;
import com.stevenmoriasi.insurance.cases.domain.Decision;
import com.stevenmoriasi.insurance.cases.domain.EvidenceMetadata;
import com.stevenmoriasi.insurance.cases.domain.Party;
import com.stevenmoriasi.insurance.cases.domain.PolicyContext;
import com.stevenmoriasi.insurance.cases.domain.Settlement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api/v1/claims")
public class CaseController {

    private final CaseManagementService caseManagement;

    public CaseController(CaseManagementService caseManagement) {
        this.caseManagement = caseManagement;
    }

    @PostMapping
    public ResponseEntity<ClaimSummary> reportClaim(
            @Valid @RequestBody ReportClaimRequest request, Authentication authentication) {
        ClaimCase claim = caseManagement.reportClaim(request.toCommand(), actor(authentication));
        return ResponseEntity.created(
                        URI.create("/internal/api/v1/claims/" + claim.getClaimReference()))
                .body(ClaimSummary.from(claim));
    }

    @GetMapping("/mine")
    public List<ClaimSummary> ownedCases(Authentication authentication) {
        return caseManagement.getOwnedCases(actor(authentication)).stream()
                .map(ClaimSummary::from)
                .toList();
    }

    @GetMapping("/{claimReference}")
    public CaseView getCase(@PathVariable String claimReference, Authentication authentication) {
        return CaseView.from(caseManagement.getCase(claimReference, actor(authentication)));
    }

    @PostMapping("/{claimReference}/tasks")
    public ResponseEntity<TaskView> createTask(
            @PathVariable String claimReference,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {
        CaseTask task =
                caseManagement.createTask(
                        claimReference, request.toCommand(), actor(authentication));
        return ResponseEntity.created(
                        URI.create(
                                "/internal/api/v1/claims/"
                                        + claimReference
                                        + "/tasks/"
                                        + task.getId()))
                .body(TaskView.from(task));
    }

    @PostMapping("/{claimReference}/tasks/{taskId}/complete")
    public TaskView completeTask(
            @PathVariable String claimReference,
            @PathVariable UUID taskId,
            Authentication authentication) {
        return TaskView.from(
                caseManagement.completeTask(claimReference, taskId, actor(authentication)));
    }

    @PostMapping("/{claimReference}/evidence")
    public ResponseEntity<EvidenceView> recordEvidence(
            @PathVariable String claimReference,
            @Valid @RequestBody RecordEvidenceRequest request,
            Authentication authentication) {
        EvidenceMetadata metadata =
                caseManagement.recordEvidence(
                        claimReference, request.toCommand(), actor(authentication));
        return ResponseEntity.created(
                        URI.create(
                                "/internal/api/v1/claims/"
                                        + claimReference
                                        + "/evidence/"
                                        + metadata.getId()))
                .body(EvidenceView.from(metadata));
    }

    @PostMapping("/{claimReference}/assessments")
    public ResponseEntity<AssessmentView> submitAssessment(
            @PathVariable String claimReference,
            @Valid @RequestBody SubmitAssessmentRequest request,
            Authentication authentication) {
        Assessment assessment =
                caseManagement.submitAssessment(
                        claimReference, request.toCommand(), actor(authentication));
        return ResponseEntity.created(
                        URI.create(
                                "/internal/api/v1/claims/"
                                        + claimReference
                                        + "/assessments/"
                                        + assessment.getId()))
                .body(AssessmentView.from(assessment));
    }

    @PostMapping("/{claimReference}/decisions")
    public ResponseEntity<DecisionView> requestDecision(
            @PathVariable String claimReference,
            @Valid @RequestBody RequestDecisionRequest request,
            Authentication authentication) {
        Decision decision =
                caseManagement.requestDecision(
                        claimReference, request.toCommand(), actor(authentication));
        return ResponseEntity.created(
                        URI.create(
                                "/internal/api/v1/claims/"
                                        + claimReference
                                        + "/decisions/"
                                        + decision.getId()))
                .body(DecisionView.from(decision));
    }

    @PostMapping("/{claimReference}/decisions/{decisionId}/approve")
    public DecisionView approveDecision(
            @PathVariable String claimReference,
            @PathVariable UUID decisionId,
            Authentication authentication) {
        return DecisionView.from(
                caseManagement.approveDecision(claimReference, decisionId, actor(authentication)));
    }

    @PostMapping("/{claimReference}/settlements")
    public ResponseEntity<SettlementView> createSettlement(
            @PathVariable String claimReference,
            @Valid @RequestBody CreateSettlementRequest request,
            Authentication authentication) {
        Settlement settlement =
                caseManagement.createSettlement(
                        claimReference, request.decisionId(), actor(authentication));
        return ResponseEntity.created(
                        URI.create(
                                "/internal/api/v1/claims/"
                                        + claimReference
                                        + "/settlements/"
                                        + settlement.getId()))
                .body(SettlementView.from(settlement));
    }

    @PostMapping("/{claimReference}/settlements/{settlementId}/instruct")
    public SettlementView instructSettlement(
            @PathVariable String claimReference,
            @PathVariable UUID settlementId,
            @Valid @RequestBody InstructSettlementRequest request,
            Authentication authentication) {
        return SettlementView.from(
                caseManagement.markSettlementInstructed(
                        claimReference,
                        settlementId,
                        request.externalReference(),
                        actor(authentication)));
    }

    @PostMapping("/{claimReference}/settlements/{settlementId}/reconcile")
    public SettlementView reconcileSettlement(
            @PathVariable String claimReference,
            @PathVariable UUID settlementId,
            Authentication authentication) {
        return SettlementView.from(
                caseManagement.reconcileSettlement(
                        claimReference, settlementId, actor(authentication)));
    }

    private static CaseActor actor(Authentication authentication) {
        String username = authentication.getName();
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                username = preferredUsername;
            }
        }
        Set<String> roles =
                authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .collect(Collectors.toUnmodifiableSet());
        return new CaseActor(username, roles);
    }

    public record ReportClaimRequest(
            @NotBlank @Size(max = 64) String claimReference,
            @NotBlank @Size(max = 64) String policyNumber,
            @NotBlank @Size(max = 64) String productCode,
            @NotBlank @Size(max = 32) String policyStatus,
            @NotNull LocalDate coverStartDate,
            @NotNull LocalDate coverEndDate,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotBlank @Size(max = 96) String claimantExternalReference,
            @NotBlank @Size(max = 160) String claimantName,
            @Size(max = 32) String claimantPhoneNumber,
            @Email @Size(max = 254) String claimantEmailAddress,
            @NotNull @PastOrPresent LocalDate lossDate) {

        ReportClaim toCommand() {
            return new ReportClaim(
                    claimReference,
                    policyNumber,
                    productCode,
                    policyStatus,
                    coverStartDate,
                    coverEndDate,
                    currency,
                    claimantExternalReference,
                    claimantName,
                    claimantPhoneNumber,
                    claimantEmailAddress,
                    lossDate);
        }
    }

    public record CreateTaskRequest(
            @NotBlank @Size(max = 64) String taskType,
            @Size(max = 128) String assignee,
            @NotNull @Future Instant dueAt) {

        CreateTask toCommand() {
            return new CreateTask(taskType, assignee, dueAt);
        }
    }

    public record RecordEvidenceRequest(
            @NotBlank @Size(max = 64) String evidenceType,
            @NotBlank @Size(max = 512) String objectKey,
            @NotBlank @Size(max = 128) String mediaType,
            @Positive long sizeBytes,
            @NotBlank @Pattern(regexp = "[a-fA-F0-9]{64}") String sha256) {

        RecordEvidence toCommand() {
            return new RecordEvidence(
                    evidenceType, objectKey, mediaType, sizeBytes, sha256.toLowerCase());
        }
    }

    public record SubmitAssessmentRequest(
            @NotNull @Positive BigDecimal recommendedAmount,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotNull AssessmentOutcome outcome,
            @NotBlank @Size(max = 2000) String rationale) {

        SubmitAssessment toCommand() {
            return new SubmitAssessment(recommendedAmount, currency, outcome, rationale);
        }
    }

    public record RequestDecisionRequest(
            @NotNull UUID assessmentId,
            @NotNull @Positive BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotBlank @Size(max = 2000) String reason) {

        RequestDecision toCommand() {
            return new RequestDecision(assessmentId, amount, currency, reason);
        }
    }

    public record CreateSettlementRequest(@NotNull UUID decisionId) {}

    public record InstructSettlementRequest(@NotBlank @Size(max = 128) String externalReference) {}

    public record ClaimSummary(
            UUID id,
            String claimReference,
            String status,
            String ownerUsername,
            LocalDate lossDate,
            Instant reportedAt,
            Instant updatedAt,
            long version) {

        static ClaimSummary from(ClaimCase claim) {
            return new ClaimSummary(
                    claim.getId(),
                    claim.getClaimReference(),
                    claim.getStatus().name(),
                    claim.getOwnerUsername(),
                    claim.getLossDate(),
                    claim.getReportedAt(),
                    claim.getUpdatedAt(),
                    claim.getVersion());
        }
    }

    public record CaseView(
            ClaimSummary claim,
            PolicyView policy,
            PartyView claimant,
            List<TaskView> tasks,
            List<EvidenceView> evidence,
            List<AssessmentView> assessments,
            List<DecisionView> decisions,
            List<SettlementView> settlements,
            List<AuditView> auditEvents) {

        static CaseView from(CaseSnapshot snapshot) {
            return new CaseView(
                    ClaimSummary.from(snapshot.claim()),
                    PolicyView.from(snapshot.policy()),
                    PartyView.from(snapshot.claimant()),
                    snapshot.tasks().stream().map(TaskView::from).toList(),
                    snapshot.evidence().stream().map(EvidenceView::from).toList(),
                    snapshot.assessments().stream().map(AssessmentView::from).toList(),
                    snapshot.decisions().stream().map(DecisionView::from).toList(),
                    snapshot.settlements().stream().map(SettlementView::from).toList(),
                    snapshot.auditEvents().stream().map(AuditView::from).toList());
        }
    }

    public record PolicyView(
            UUID id,
            String policyNumber,
            String productCode,
            String status,
            LocalDate coverStartDate,
            LocalDate coverEndDate,
            String currency) {

        static PolicyView from(PolicyContext policy) {
            return new PolicyView(
                    policy.getId(),
                    policy.getPolicyNumber(),
                    policy.getProductCode(),
                    policy.getStatus(),
                    policy.getCoverStartDate(),
                    policy.getCoverEndDate(),
                    policy.getCurrency());
        }
    }

    public record PartyView(
            UUID id,
            String partyType,
            String externalReference,
            String fullName,
            String phoneNumber,
            String emailAddress) {

        static PartyView from(Party party) {
            return new PartyView(
                    party.getId(),
                    party.getPartyType().name(),
                    party.getExternalReference(),
                    party.getFullName(),
                    party.getPhoneNumber(),
                    party.getEmailAddress());
        }
    }

    public record TaskView(
            UUID id,
            String taskType,
            String status,
            String assignee,
            Instant dueAt,
            Instant createdAt,
            Instant completedAt) {

        static TaskView from(CaseTask task) {
            return new TaskView(
                    task.getId(),
                    task.getTaskType(),
                    task.getStatus().name(),
                    task.getAssignee(),
                    task.getDueAt(),
                    task.getCreatedAt(),
                    task.getCompletedAt());
        }
    }

    public record EvidenceView(
            UUID id,
            String evidenceType,
            String objectKey,
            String mediaType,
            long sizeBytes,
            String sha256,
            String status,
            Instant submittedAt) {

        static EvidenceView from(EvidenceMetadata evidence) {
            return new EvidenceView(
                    evidence.getId(),
                    evidence.getEvidenceType(),
                    evidence.getObjectKey(),
                    evidence.getMediaType(),
                    evidence.getSizeBytes(),
                    evidence.getSha256(),
                    evidence.getStatus().name(),
                    evidence.getSubmittedAt());
        }
    }

    public record AssessmentView(
            UUID id,
            String assessorUsername,
            BigDecimal recommendedAmount,
            String currency,
            String outcome,
            String rationale,
            Instant submittedAt) {

        static AssessmentView from(Assessment assessment) {
            return new AssessmentView(
                    assessment.getId(),
                    assessment.getAssessorUsername(),
                    assessment.getRecommendedAmount(),
                    assessment.getCurrency(),
                    assessment.getOutcome().name(),
                    assessment.getRationale(),
                    assessment.getSubmittedAt());
        }
    }

    public record DecisionView(
            UUID id,
            UUID assessmentId,
            String requestedBy,
            BigDecimal amount,
            String currency,
            String status,
            String decidedBy,
            String reason,
            Instant requestedAt,
            Instant decidedAt) {

        static DecisionView from(Decision decision) {
            return new DecisionView(
                    decision.getId(),
                    decision.getAssessmentId(),
                    decision.getRequestedBy(),
                    decision.getAmount(),
                    decision.getCurrency(),
                    decision.getStatus().name(),
                    decision.getDecidedBy(),
                    decision.getReason(),
                    decision.getRequestedAt(),
                    decision.getDecidedAt());
        }
    }

    public record SettlementView(
            UUID id,
            UUID decisionId,
            BigDecimal amount,
            String currency,
            String status,
            String externalReference,
            Instant createdAt,
            Instant instructedAt,
            Instant reconciledAt) {

        static SettlementView from(Settlement settlement) {
            return new SettlementView(
                    settlement.getId(),
                    settlement.getDecisionId(),
                    settlement.getAmount(),
                    settlement.getCurrency(),
                    settlement.getStatus().name(),
                    settlement.getExternalReference(),
                    settlement.getCreatedAt(),
                    settlement.getInstructedAt(),
                    settlement.getReconciledAt());
        }
    }

    public record AuditView(
            UUID id, String eventType, String actorUsername, Instant occurredAt, String eventData) {

        static AuditView from(AuditEvent event) {
            return new AuditView(
                    event.getId(),
                    event.getEventType(),
                    event.getActorUsername(),
                    event.getOccurredAt(),
                    event.getEventData());
        }
    }
}
