package com.stevenmoriasi.insurance.workflows.brokers;

import com.stevenmoriasi.insurance.workflows.config.TemporalProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.stereotype.Service;

@Service
public class BrokerOnboardingWorkflowService {

    private static final String WORKFLOW_ID_PREFIX = "broker-onboarding-";

    private final WorkflowClient workflowClient;
    private final TemporalProperties properties;
    private final MeterRegistry meterRegistry;

    public BrokerOnboardingWorkflowService(
            WorkflowClient workflowClient,
            TemporalProperties properties,
            MeterRegistry meterRegistry) {
        this.workflowClient = workflowClient;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
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
        count("started");
        return new WorkflowReference(execution.getWorkflowId(), execution.getRunId());
    }

    public BrokerOnboardingStatus status(String brokerReference) {
        return workflow(brokerReference).status();
    }

    public void documentsReceived(String brokerReference, String evidenceReference) {
        workflow(brokerReference).documentsReceived(evidenceReference);
        count("documents_received");
    }

    public void automatedCheckCompleted(String brokerReference, String outcome) {
        workflow(brokerReference).automatedCheckCompleted(outcome);
        count(
                switch (outcome.toUpperCase()) {
                    case "CLEAR" -> "automated_check_clear";
                    case "MANUAL_REVIEW" -> "automated_check_manual_review";
                    case "REJECT" -> "automated_check_reject";
                    default -> "automated_check_invalid";
                });
    }

    public void manualReviewCompleted(String brokerReference, boolean approved) {
        workflow(brokerReference).manualReviewCompleted(approved);
        count(approved ? "manual_review_approved" : "manual_review_rejected");
    }

    public void approvalRecorded(String brokerReference, boolean approved) {
        workflow(brokerReference).approvalRecorded(approved);
        count(approved ? "approval_recorded" : "approval_rejected");
    }

    public void cancel(String brokerReference, String reason) {
        workflow(brokerReference).cancel(reason);
        count("cancelled");
    }

    private BrokerOnboardingWorkflow workflow(String brokerReference) {
        return workflowClient.newWorkflowStub(
                BrokerOnboardingWorkflow.class, workflowId(brokerReference));
    }

    private static String workflowId(String brokerReference) {
        return WORKFLOW_ID_PREFIX + brokerReference;
    }

    private void count(String operation) {
        meterRegistry
                .counter(
                        "insurance.workflow.operations",
                        "journey",
                        "broker_onboarding",
                        "operation",
                        operation)
                .increment();
    }

    public record WorkflowReference(String workflowId, String runId) {}
}
