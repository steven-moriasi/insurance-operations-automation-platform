package com.stevenmoriasi.insurance.workflows.claims;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MotorClaimWorkflow {

    @WorkflowMethod
    MotorClaimResult run(MotorClaimInput input);

    @SignalMethod
    void evidenceReceived(String evidenceReference);

    @SignalMethod
    void assessmentSubmitted(String assessmentReference);

    @SignalMethod
    void decisionRecorded(String decisionReference);

    @SignalMethod
    void settlementReconciled(String settlementReference);

    @SignalMethod
    void cancel(String reason);

    @QueryMethod
    MotorClaimStatus status();
}
