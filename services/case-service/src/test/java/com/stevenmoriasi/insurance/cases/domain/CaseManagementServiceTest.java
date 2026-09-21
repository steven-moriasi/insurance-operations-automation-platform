package com.stevenmoriasi.insurance.cases.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stevenmoriasi.insurance.cases.domain.CaseManagementService.ReportClaim;
import com.stevenmoriasi.insurance.cases.domain.CaseManagementService.RequestDecision;
import com.stevenmoriasi.insurance.cases.domain.CaseManagementService.SubmitAssessment;
import com.stevenmoriasi.insurance.cases.domain.CaseOperationException.Reason;
import com.stevenmoriasi.insurance.cases.domain.CaseTypes.AssessmentOutcome;
import com.stevenmoriasi.insurance.cases.domain.CaseTypes.ClaimStatus;
import com.stevenmoriasi.insurance.cases.domain.CaseTypes.DecisionStatus;
import com.stevenmoriasi.insurance.cases.domain.CaseTypes.SettlementStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CaseManagementServiceTest {

    @Autowired private CaseManagementService caseManagement;

    @Test
    void enforcesMakerCheckerAndFinancialAuthorityBeforeSettlement() {
        CaseActor maker =
                new CaseActor("claims.agent", Set.of("CLAIMS_OFFICER", "CLAIMS_ASSESSOR"));
        ClaimCase claim = caseManagement.reportClaim(reportClaim("CLM-2001"), maker);
        Assessment assessment =
                caseManagement.submitAssessment(
                        claim.getClaimReference(),
                        new SubmitAssessment(
                                new BigDecimal("250000.00"),
                                "KES",
                                AssessmentOutcome.RECOMMEND_APPROVAL,
                                "Synthetic repair estimate reviewed"),
                        maker);
        Decision decision =
                caseManagement.requestDecision(
                        claim.getClaimReference(),
                        new RequestDecision(
                                assessment.getId(),
                                new BigDecimal("250000.00"),
                                "KES",
                                "Within assessed repair cost"),
                        maker);

        assertThatThrownBy(
                        () ->
                                caseManagement.approveDecision(
                                        claim.getClaimReference(),
                                        decision.getId(),
                                        new CaseActor(
                                                "claims.agent", Set.of("SENIOR_CLAIMS_APPROVER"))))
                .isInstanceOfSatisfying(
                        CaseOperationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(Reason.FORBIDDEN));
        assertThatThrownBy(
                        () ->
                                caseManagement.approveDecision(
                                        claim.getClaimReference(),
                                        decision.getId(),
                                        new CaseActor("checker.one", Set.of("CLAIMS_APPROVER"))))
                .isInstanceOfSatisfying(
                        CaseOperationException.class,
                        exception ->
                                assertThat(exception.getReason())
                                        .isEqualTo(Reason.AUTHORITY_EXCEEDED));

        Decision approved =
                caseManagement.approveDecision(
                        claim.getClaimReference(),
                        decision.getId(),
                        new CaseActor("checker.senior", Set.of("SENIOR_CLAIMS_APPROVER")));
        Settlement settlement =
                caseManagement.createSettlement(
                        claim.getClaimReference(),
                        approved.getId(),
                        new CaseActor("finance.operator", Set.of("FINANCE_OPERATOR")));
        caseManagement.markSettlementInstructed(
                claim.getClaimReference(),
                settlement.getId(),
                "PAY-SYNTHETIC-2001",
                new CaseActor("finance.operator", Set.of("FINANCE_OPERATOR")));
        Settlement reconciled =
                caseManagement.reconcileSettlement(
                        claim.getClaimReference(),
                        settlement.getId(),
                        new CaseActor("finance.operator", Set.of("FINANCE_OPERATOR")));

        CaseManagementService.CaseSnapshot snapshot =
                caseManagement.getCase(
                        claim.getClaimReference(),
                        new CaseActor("claims.supervisor", Set.of("CLAIMS_SUPERVISOR")));
        assertThat(approved.getStatus()).isEqualTo(DecisionStatus.APPROVED);
        assertThat(reconciled.getStatus()).isEqualTo(SettlementStatus.RECONCILED);
        assertThat(snapshot.claim().getStatus()).isEqualTo(ClaimStatus.SETTLED);
        assertThat(snapshot.auditEvents())
                .extracting(AuditEvent::getEventType)
                .containsExactly(
                        "CLAIM_REPORTED",
                        "ASSESSMENT_SUBMITTED",
                        "DECISION_REQUESTED",
                        "DECISION_APPROVED",
                        "SETTLEMENT_CREATED",
                        "SETTLEMENT_INSTRUCTED",
                        "SETTLEMENT_RECONCILED");
    }

    @Test
    void rejectsAccessFromAnUnassignedActorWithoutAnOversightRole() {
        CaseActor owner = new CaseActor("claims.owner", Set.of("CLAIMS_OFFICER"));
        caseManagement.reportClaim(reportClaim("CLM-2002"), owner);

        assertThatThrownBy(
                        () ->
                                caseManagement.getCase(
                                        "CLM-2002",
                                        new CaseActor("claims.other", Set.of("CLAIMS_OFFICER"))))
                .isInstanceOfSatisfying(
                        CaseOperationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(Reason.FORBIDDEN));
    }

    private static ReportClaim reportClaim(String claimReference) {
        return new ReportClaim(
                claimReference,
                "POL-" + claimReference,
                "PRIVATE_MOTOR",
                "ACTIVE",
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"),
                "KES",
                "PTY-" + claimReference,
                "Amina Wanjiku",
                "+254700000001",
                "amina@example.test",
                LocalDate.parse("2026-09-20"));
    }
}
