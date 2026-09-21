package com.stevenmoriasi.insurance.workflows.renewals;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PolicyRenewalWorkflow {

    @WorkflowMethod
    RenewalResult run(RenewalInput input);

    @SignalMethod
    void customerDecision(String decision);

    @SignalMethod
    void makerCheckerDecision(boolean approved);

    @SignalMethod
    void paymentReconciled(String paymentReference);

    @SignalMethod
    void cancel(String reason);

    @QueryMethod
    RenewalStatus status();
}
