package com.stevenmoriasi.insurance.workflows.brokers;

import com.stevenmoriasi.insurance.workflows.config.TemporalProperties;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.stereotype.Service;

@Service
public class BrokerOnboardingWorkflowService {

    private static final String WORKFLOW_ID_PREFIX = "broker-onboarding-";

    private final WorkflowClient workflowClient;
    private final TemporalProperties properties;

    public BrokerOnboardingWorkflowService(
            WorkflowClient workflowClient, TemporalProperties properties) {
        this.workflowClient = workflowClient;
        this.properties = properties;
    }

    public WorkflowReference start(BrokerOnboardingInput input) {
        BrokerOnboardingWorkflow workflow =
                workflowClient.newWorkflowStub(
                        BrokerOnboardingWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(workflowId(input.brokerReference()))
                                .setTaskQueue(properties.taskQueue())
                                .build());
        WorkflowExecution execution = WorkflowClient.start(workflow::run, input);
        return new WorkflowReference(execution.getWorkflowId(), execution.getRunId());
    }

    public BrokerOnboardingStatus status(String brokerReference) {
        return workflow(brokerReference).status();
    }

    public void documentsReceived(String brokerReference, String evidenceReference) {
        workflow(brokerReference).documentsReceived(evidenceReference);
    }

    public void automatedCheckCompleted(String brokerReference, String outcome) {
        workflow(brokerReference).automatedCheckCompleted(outcome);
    }

    public void manualReviewCompleted(String brokerReference, boolean approved) {
        workflow(brokerReference).manualReviewCompleted(approved);
    }

    public void approvalRecorded(String brokerReference, boolean approved) {
        workflow(brokerReference).approvalRecorded(approved);
    }

    public void cancel(String brokerReference, String reason) {
        workflow(brokerReference).cancel(reason);
    }

    private BrokerOnboardingWorkflow workflow(String brokerReference) {
        return workflowClient.newWorkflowStub(
                BrokerOnboardingWorkflow.class, workflowId(brokerReference));
    }

    private static String workflowId(String brokerReference) {
        return WORKFLOW_ID_PREFIX + brokerReference;
    }

    public record WorkflowReference(String workflowId, String runId) {}
}
