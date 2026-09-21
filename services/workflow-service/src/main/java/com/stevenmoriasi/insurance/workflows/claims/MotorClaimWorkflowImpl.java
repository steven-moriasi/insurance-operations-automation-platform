package com.stevenmoriasi.insurance.workflows.claims;

import static io.temporal.workflow.Workflow.DEFAULT_VERSION;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.common.SearchAttributeKey;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class MotorClaimWorkflowImpl implements MotorClaimWorkflow {

    private static final SearchAttributeKey<String> CLAIM_REFERENCE =
            SearchAttributeKey.forKeyword("InsuranceClaimReference");
    private static final SearchAttributeKey<String> WORKFLOW_STAGE =
            SearchAttributeKey.forKeyword("InsuranceWorkflowStage");

    private final ClaimActivities activities =
            Workflow.newActivityStub(
                    ClaimActivities.class,
                    ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(30))
                            .setRetryOptions(
                                    RetryOptions.newBuilder()
                                            .setInitialInterval(Duration.ofSeconds(1))
                                            .setBackoffCoefficient(2)
                                            .setMaximumInterval(Duration.ofSeconds(10))
                                            .setMaximumAttempts(4)
                                            .setDoNotRetry("BUSINESS_RULE_FAILURE")
                                            .build())
                            .build());

    private ClaimWorkflowStage stage = ClaimWorkflowStage.VERIFYING_COVERAGE;
    private final List<String> overdueStages = new ArrayList<>();
    private String currentTaskId;
    private String cancellationReason;
    private boolean evidenceReceived;
    private boolean assessmentSubmitted;
    private boolean decisionRecorded;
    private boolean settlementReconciled;
    private String claimReference;

    @Override
    public MotorClaimResult run(MotorClaimInput input) {
        validate(input);
        claimReference = input.claimReference();
        Workflow.getVersion("motor-claim-workflow-v1", DEFAULT_VERSION, 1);
        Workflow.upsertTypedSearchAttributes(CLAIM_REFERENCE.valueSet(claimReference));

        activities.verifyCoverage(claimReference);
        if (cancelled()) {
            return compensate();
        }

        openStage(
                ClaimWorkflowStage.AWAITING_EVIDENCE,
                "EVIDENCE_COLLECTION",
                "CLAIMS_OFFICER",
                input.evidenceSlaSeconds());
        awaitHumanStep("EVIDENCE", input.evidenceSlaSeconds(), () -> evidenceReceived);
        if (cancelled()) {
            return compensate();
        }

        openStage(
                ClaimWorkflowStage.AWAITING_ASSESSMENT,
                "CLAIM_ASSESSMENT",
                "CLAIMS_ASSESSOR",
                input.assessmentSlaSeconds());
        awaitHumanStep("ASSESSMENT", input.assessmentSlaSeconds(), () -> assessmentSubmitted);
        if (cancelled()) {
            return compensate();
        }

        openStage(
                ClaimWorkflowStage.AWAITING_DECISION,
                "CLAIM_DECISION",
                "CLAIMS_APPROVER",
                input.decisionSlaSeconds());
        awaitHumanStep("DECISION", input.decisionSlaSeconds(), () -> decisionRecorded);
        if (cancelled()) {
            return compensate();
        }

        openStage(
                ClaimWorkflowStage.AWAITING_SETTLEMENT,
                "SETTLEMENT_RECONCILIATION",
                "FINANCE_OPERATOR",
                input.settlementSlaSeconds());
        awaitHumanStep("SETTLEMENT", input.settlementSlaSeconds(), () -> settlementReconciled);
        if (cancelled()) {
            return compensate();
        }

        currentTaskId = null;
        moveTo(ClaimWorkflowStage.COMPLETED);
        activities.recordCompletion(claimReference);
        return result();
    }

    @Override
    public void evidenceReceived(String evidenceReference) {
        evidenceReceived = present(evidenceReference);
    }

    @Override
    public void assessmentSubmitted(String assessmentReference) {
        assessmentSubmitted = present(assessmentReference);
    }

    @Override
    public void decisionRecorded(String decisionReference) {
        decisionRecorded = present(decisionReference);
    }

    @Override
    public void settlementReconciled(String settlementReference) {
        settlementReconciled = present(settlementReference);
    }

    @Override
    public void cancel(String reason) {
        cancellationReason = present(reason) ? reason : "Cancelled by operator";
    }

    @Override
    public MotorClaimStatus status() {
        return new MotorClaimStatus(
                stage, List.copyOf(overdueStages), currentTaskId, cancellationReason);
    }

    private void openStage(
            ClaimWorkflowStage nextStage, String taskType, String role, long slaSeconds) {
        moveTo(nextStage);
        currentTaskId =
                activities.createTask(
                        claimReference,
                        taskType,
                        role,
                        Workflow.currentTimeMillis() + Duration.ofSeconds(slaSeconds).toMillis());
    }

    private void awaitHumanStep(String overdueStage, long slaSeconds, BooleanSupplier completed) {
        boolean completedWithinSla =
                Workflow.await(
                        Duration.ofSeconds(slaSeconds),
                        () -> completed.getAsBoolean() || cancelled());
        if (!completedWithinSla) {
            overdueStages.add(overdueStage);
            activities.escalate(claimReference, overdueStage, currentTaskId);
            Workflow.upsertTypedSearchAttributes(
                    WORKFLOW_STAGE.valueSet(stage.name() + "_OVERDUE"));
            Workflow.await(() -> completed.getAsBoolean() || cancelled());
        }
    }

    private MotorClaimResult compensate() {
        if (currentTaskId != null) {
            activities.cancelTask(claimReference, currentTaskId, cancellationReason);
            currentTaskId = null;
        }
        moveTo(ClaimWorkflowStage.CANCELLED);
        return result();
    }

    private void moveTo(ClaimWorkflowStage nextStage) {
        stage = nextStage;
        Workflow.upsertTypedSearchAttributes(WORKFLOW_STAGE.valueSet(stage.name()));
    }

    private MotorClaimResult result() {
        return new MotorClaimResult(claimReference, stage, List.copyOf(overdueStages));
    }

    private boolean cancelled() {
        return cancellationReason != null;
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static void validate(MotorClaimInput input) {
        if (input == null || !present(input.claimReference())) {
            throw new IllegalArgumentException("claimReference is required");
        }
        if (input.evidenceSlaSeconds() <= 0
                || input.assessmentSlaSeconds() <= 0
                || input.decisionSlaSeconds() <= 0
                || input.settlementSlaSeconds() <= 0) {
            throw new IllegalArgumentException("All workflow SLA values must be positive");
        }
    }
}
