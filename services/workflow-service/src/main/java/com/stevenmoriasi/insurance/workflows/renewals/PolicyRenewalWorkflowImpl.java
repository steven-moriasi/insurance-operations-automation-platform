package com.stevenmoriasi.insurance.workflows.renewals;

import static io.temporal.workflow.Workflow.DEFAULT_VERSION;

import com.stevenmoriasi.insurance.workflows.shared.ExceptionRoute;
import com.stevenmoriasi.insurance.workflows.shared.HumanTaskStep;
import io.temporal.common.SearchAttributeKey;
import io.temporal.workflow.Workflow;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class PolicyRenewalWorkflowImpl implements PolicyRenewalWorkflow {

    private static final SearchAttributeKey<String> WORKFLOW_STAGE =
            SearchAttributeKey.forKeyword("InsuranceWorkflowStage");

    private final HumanTaskStep task = new HumanTaskStep();
    private final List<ExceptionRoute> exceptionRoutes = new ArrayList<>();
    private RenewalWorkflowStage stage = RenewalWorkflowStage.AWAITING_CUSTOMER_DECISION;
    private String policyNumber;
    private String customerDecision;
    private Boolean makerCheckerApproved;
    private String paymentReference;
    private String cancellationReason;

    @Override
    public RenewalResult run(RenewalInput input) {
        validate(input);
        policyNumber = input.policyNumber();
        Workflow.getVersion("policy-renewal-workflow-v1", DEFAULT_VERSION, 1);

        open(
                RenewalWorkflowStage.AWAITING_CUSTOMER_DECISION,
                "RENEWAL_DECISION",
                "POLICY_HOLDER",
                input.customerDecisionSlaSeconds());
        await(
                "CUSTOMER_DECISION",
                input.customerDecisionSlaSeconds(),
                () -> customerDecision != null);
        if (cancelled()) {
            return finish(RenewalWorkflowStage.CANCELLED);
        }
        if ("DECLINE".equals(customerDecision)) {
            return finish(RenewalWorkflowStage.DECLINED);
        }

        if (input.materialChangeRequired()) {
            open(
                    RenewalWorkflowStage.AWAITING_MAKER_CHECKER,
                    "RENEWAL_MATERIAL_CHANGE_APPROVAL",
                    "UNDERWRITING_APPROVER",
                    input.makerCheckerSlaSeconds());
            await(
                    "MAKER_CHECKER",
                    input.makerCheckerSlaSeconds(),
                    () -> makerCheckerApproved != null);
            if (cancelled()) {
                return finish(RenewalWorkflowStage.CANCELLED);
            }
            if (!Boolean.TRUE.equals(makerCheckerApproved)) {
                route("MAKER_CHECKER", "PROCESS_OWNER", "MATERIAL_CHANGE_REJECTED");
                return finish(RenewalWorkflowStage.DECLINED);
            }
        }

        open(
                RenewalWorkflowStage.AWAITING_PAYMENT,
                "RENEWAL_PAYMENT",
                "FINANCE_OPERATOR",
                input.paymentSlaSeconds());
        await("PAYMENT", input.paymentSlaSeconds(), () -> paymentReference != null);
        return cancelled()
                ? finish(RenewalWorkflowStage.CANCELLED)
                : finish(RenewalWorkflowStage.COMPLETED);
    }

    @Override
    public void customerDecision(String decision) {
        if ("RENEW".equals(decision) || "DECLINE".equals(decision)) {
            customerDecision = decision;
        }
    }

    @Override
    public void makerCheckerDecision(boolean approved) {
        makerCheckerApproved = approved;
    }

    @Override
    public void paymentReconciled(String paymentReference) {
        if (present(paymentReference)) {
            this.paymentReference = paymentReference;
        }
    }

    @Override
    public void cancel(String reason) {
        cancellationReason = present(reason) ? reason : "Cancelled by operator";
    }

    @Override
    public RenewalStatus status() {
        return new RenewalStatus(
                stage, task.view(), List.copyOf(exceptionRoutes), cancellationReason);
    }

    private void open(
            RenewalWorkflowStage nextStage,
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

    private RenewalResult finish(RenewalWorkflowStage outcome) {
        stage = outcome;
        task.close();
        Workflow.upsertTypedSearchAttributes(WORKFLOW_STAGE.valueSet(stage.name()));
        return new RenewalResult(policyNumber, outcome, List.copyOf(exceptionRoutes));
    }

    private boolean cancelled() {
        return cancellationReason != null;
    }

    private static void validate(RenewalInput input) {
        if (input == null || !present(input.policyNumber())) {
            throw new IllegalArgumentException("policyNumber is required");
        }
        if (input.customerDecisionSlaSeconds() <= 0
                || input.makerCheckerSlaSeconds() <= 0
                || input.paymentSlaSeconds() <= 0) {
            throw new IllegalArgumentException("All workflow SLA values must be positive");
        }
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
