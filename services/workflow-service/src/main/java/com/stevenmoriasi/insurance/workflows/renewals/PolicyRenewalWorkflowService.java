package com.stevenmoriasi.insurance.workflows.renewals;

import com.stevenmoriasi.insurance.workflows.config.TemporalProperties;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.stereotype.Service;

@Service
public class PolicyRenewalWorkflowService {

    private static final String WORKFLOW_ID_PREFIX = "policy-renewal-";

    private final WorkflowClient workflowClient;
    private final TemporalProperties properties;

    public PolicyRenewalWorkflowService(
            WorkflowClient workflowClient, TemporalProperties properties) {
        this.workflowClient = workflowClient;
        this.properties = properties;
    }

    public WorkflowReference start(RenewalInput input) {
        PolicyRenewalWorkflow workflow =
                workflowClient.newWorkflowStub(
                        PolicyRenewalWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(workflowId(input.policyNumber()))
                                .setTaskQueue(properties.taskQueue())
                                .build());
        WorkflowExecution execution = WorkflowClient.start(workflow::run, input);
        return new WorkflowReference(execution.getWorkflowId(), execution.getRunId());
    }

    public RenewalStatus status(String policyNumber) {
        return workflow(policyNumber).status();
    }

    public void customerDecision(String policyNumber, String decision) {
        workflow(policyNumber).customerDecision(decision);
    }

    public void makerCheckerDecision(String policyNumber, boolean approved) {
        workflow(policyNumber).makerCheckerDecision(approved);
    }

    public void paymentReconciled(String policyNumber, String paymentReference) {
        workflow(policyNumber).paymentReconciled(paymentReference);
    }

    public void cancel(String policyNumber, String reason) {
        workflow(policyNumber).cancel(reason);
    }

    private PolicyRenewalWorkflow workflow(String policyNumber) {
        return workflowClient.newWorkflowStub(
                PolicyRenewalWorkflow.class, workflowId(policyNumber));
    }

    private static String workflowId(String policyNumber) {
        return WORKFLOW_ID_PREFIX + policyNumber;
    }

    public record WorkflowReference(String workflowId, String runId) {}
}
