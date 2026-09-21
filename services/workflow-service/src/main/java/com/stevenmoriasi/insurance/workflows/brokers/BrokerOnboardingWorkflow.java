package com.stevenmoriasi.insurance.workflows.brokers;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface BrokerOnboardingWorkflow {

    @WorkflowMethod
    BrokerOnboardingResult run(BrokerOnboardingInput input);

    @SignalMethod
    void documentsReceived(String evidenceReference);

    @SignalMethod
    void automatedCheckCompleted(String outcome);

    @SignalMethod
    void manualReviewCompleted(boolean approved);

    @SignalMethod
    void approvalRecorded(boolean approved);

    @SignalMethod
    void cancel(String reason);

    @QueryMethod
    BrokerOnboardingStatus status();
}
