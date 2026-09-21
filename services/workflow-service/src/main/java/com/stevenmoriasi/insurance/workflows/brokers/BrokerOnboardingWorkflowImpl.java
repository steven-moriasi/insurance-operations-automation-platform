package com.stevenmoriasi.insurance.workflows.brokers;

import static io.temporal.workflow.Workflow.DEFAULT_VERSION;

import com.stevenmoriasi.insurance.workflows.shared.ExceptionRoute;
import com.stevenmoriasi.insurance.workflows.shared.HumanTaskStep;
import io.temporal.common.SearchAttributeKey;
import io.temporal.workflow.Workflow;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class BrokerOnboardingWorkflowImpl implements BrokerOnboardingWorkflow {

    private static final SearchAttributeKey<String> WORKFLOW_STAGE =
            SearchAttributeKey.forKeyword("InsuranceWorkflowStage");

    private final HumanTaskStep task = new HumanTaskStep();
    private final List<ExceptionRoute> exceptionRoutes = new ArrayList<>();
    private BrokerOnboardingStage stage = BrokerOnboardingStage.AWAITING_DOCUMENTS;
    private String brokerReference;
    private String evidenceReference;
    private String automatedOutcome;
    private Boolean manualReviewApproved;
    private Boolean approvalRecorded;
    private String cancellationReason;

    @Override
    public BrokerOnboardingResult run(BrokerOnboardingInput input) {
        validate(input);
        brokerReference = input.brokerReference();
        Workflow.getVersion("broker-onboarding-workflow-v1", DEFAULT_VERSION, 1);

        open(
                BrokerOnboardingStage.AWAITING_DOCUMENTS,
                "BROKER_DOCUMENT_COLLECTION",
                "BROKER_ONBOARDING_OFFICER",
                input.documentSlaSeconds());
        await("DOCUMENTS", input.documentSlaSeconds(), () -> evidenceReference != null);
        if (cancelled()) {
            return finish(BrokerOnboardingStage.CANCELLED);
        }

        open(
                BrokerOnboardingStage.AWAITING_AUTOMATED_CHECK,
                "BROKER_PORTAL_CHECK",
                "AUTOMATION_WORKER",
                input.automatedCheckSlaSeconds());
        await("AUTOMATED_CHECK", input.automatedCheckSlaSeconds(), () -> automatedOutcome != null);
        if (cancelled()) {
            return finish(BrokerOnboardingStage.CANCELLED);
        }
        if ("REJECT".equals(automatedOutcome)) {
            route("AUTOMATED_CHECK", "PROCESS_OWNER", "AUTOMATED_CHECK_REJECTED");
            return finish(BrokerOnboardingStage.REJECTED);
        }

        if ("MANUAL_REVIEW".equals(automatedOutcome)) {
            open(
                    BrokerOnboardingStage.AWAITING_MANUAL_REVIEW,
                    "BROKER_COMPLIANCE_REVIEW",
                    "COMPLIANCE_REVIEWER",
                    input.manualReviewSlaSeconds());
            await(
                    "MANUAL_REVIEW",
                    input.manualReviewSlaSeconds(),
                    () -> manualReviewApproved != null);
            if (cancelled()) {
                return finish(BrokerOnboardingStage.CANCELLED);
            }
            if (!Boolean.TRUE.equals(manualReviewApproved)) {
                route("MANUAL_REVIEW", "PROCESS_OWNER", "MANUAL_REVIEW_REJECTED");
                return finish(BrokerOnboardingStage.REJECTED);
            }
        }

        open(
                BrokerOnboardingStage.AWAITING_APPROVAL,
                "BROKER_ONBOARDING_APPROVAL",
                "BROKER_ONBOARDING_APPROVER",
                input.approvalSlaSeconds());
        await("APPROVAL", input.approvalSlaSeconds(), () -> approvalRecorded != null);
        if (cancelled()) {
            return finish(BrokerOnboardingStage.CANCELLED);
        }
        if (!Boolean.TRUE.equals(approvalRecorded)) {
            route("APPROVAL", "PROCESS_OWNER", "APPROVAL_REJECTED");
            return finish(BrokerOnboardingStage.REJECTED);
        }
        return finish(BrokerOnboardingStage.COMPLETED);
    }

    @Override
    public void documentsReceived(String evidenceReference) {
        if (present(evidenceReference)) {
            this.evidenceReference = evidenceReference;
        }
    }

    @Override
    public void automatedCheckCompleted(String outcome) {
        if ("CLEAR".equals(outcome)
                || "MANUAL_REVIEW".equals(outcome)
                || "REJECT".equals(outcome)) {
            automatedOutcome = outcome;
        }
    }

    @Override
    public void manualReviewCompleted(boolean approved) {
        manualReviewApproved = approved;
    }

    @Override
    public void approvalRecorded(boolean approved) {
        approvalRecorded = approved;
    }

    @Override
    public void cancel(String reason) {
        cancellationReason = present(reason) ? reason : "Cancelled by operator";
    }

    @Override
    public BrokerOnboardingStatus status() {
        return new BrokerOnboardingStatus(
                stage, task.view(), List.copyOf(exceptionRoutes), cancellationReason);
    }

    private void open(
            BrokerOnboardingStage nextStage,
            String taskType,
            String candidateRole,
            long slaSeconds) {
        stage = nextStage;
        Workflow.upsertTypedSearchAttributes(WORKFLOW_STAGE.valueSet(stage.name()));
        task.open(taskType, candidateRole, slaSeconds);
    }

    private void await(String routeStage, long slaSeconds, BooleanSupplier completed) {
        if (!task.await(slaSeconds, completed, this::cancelled)) {
            route(routeStage, "PROCESS_OWNER", "SLA_BREACH");
            task.awaitWithoutTimeout(completed, this::cancelled);
        }
    }

    private void route(String routeStage, String ownerRole, String reason) {
        exceptionRoutes.add(new ExceptionRoute(routeStage, ownerRole, reason));
    }

    private BrokerOnboardingResult finish(BrokerOnboardingStage outcome) {
        stage = outcome;
        task.close();
        Workflow.upsertTypedSearchAttributes(WORKFLOW_STAGE.valueSet(stage.name()));
        return new BrokerOnboardingResult(brokerReference, outcome, List.copyOf(exceptionRoutes));
    }

    private boolean cancelled() {
        return cancellationReason != null;
    }

    private static void validate(BrokerOnboardingInput input) {
        if (input == null || !present(input.brokerReference())) {
            throw new IllegalArgumentException("brokerReference is required");
        }
        if (input.documentSlaSeconds() <= 0
                || input.automatedCheckSlaSeconds() <= 0
                || input.manualReviewSlaSeconds() <= 0
                || input.approvalSlaSeconds() <= 0) {
            throw new IllegalArgumentException("All workflow SLA values must be positive");
        }
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
