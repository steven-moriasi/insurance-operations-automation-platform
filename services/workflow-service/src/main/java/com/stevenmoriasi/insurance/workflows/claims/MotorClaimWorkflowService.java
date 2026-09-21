package com.stevenmoriasi.insurance.workflows.claims;

import com.stevenmoriasi.insurance.workflows.config.TemporalProperties;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.stereotype.Service;

@Service
public class MotorClaimWorkflowService {

    private static final String WORKFLOW_ID_PREFIX = "motor-claim-";

    private final WorkflowClient workflowClient;
    private final TemporalProperties properties;

    public MotorClaimWorkflowService(WorkflowClient workflowClient, TemporalProperties properties) {
        this.workflowClient = workflowClient;
        this.properties = properties;
    }

    public WorkflowReference start(MotorClaimInput input) {
        MotorClaimWorkflow workflow =
                workflowClient.newWorkflowStub(
                        MotorClaimWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(workflowId(input.claimReference()))
                                .setTaskQueue(properties.taskQueue())
                                .build());
        WorkflowExecution execution = WorkflowClient.start(workflow::run, input);
        return new WorkflowReference(execution.getWorkflowId(), execution.getRunId());
    }

    public MotorClaimStatus status(String claimReference) {
        return workflow(claimReference).status();
    }

    public void evidenceReceived(String claimReference, String evidenceReference) {
        workflow(claimReference).evidenceReceived(evidenceReference);
    }

    public void assessmentSubmitted(String claimReference, String assessmentReference) {
        workflow(claimReference).assessmentSubmitted(assessmentReference);
    }

    public void decisionRecorded(String claimReference, String decisionReference) {
        workflow(claimReference).decisionRecorded(decisionReference);
    }

    public void settlementReconciled(String claimReference, String settlementReference) {
        workflow(claimReference).settlementReconciled(settlementReference);
    }

    public void cancel(String claimReference, String reason) {
        workflow(claimReference).cancel(reason);
    }

    private MotorClaimWorkflow workflow(String claimReference) {
        return workflowClient.newWorkflowStub(MotorClaimWorkflow.class, workflowId(claimReference));
    }

    private static String workflowId(String claimReference) {
        return WORKFLOW_ID_PREFIX + claimReference;
    }

    public record WorkflowReference(String workflowId, String runId) {}
}
