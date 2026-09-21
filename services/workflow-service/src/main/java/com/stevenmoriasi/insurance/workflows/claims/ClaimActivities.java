package com.stevenmoriasi.insurance.workflows.claims;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ClaimActivities {

    @ActivityMethod
    void verifyCoverage(String claimReference);

    @ActivityMethod
    String createTask(
            String claimReference, String taskType, String assigneeRole, long dueAtEpochMillis);

    @ActivityMethod
    void escalate(String claimReference, String stage, String taskId);

    @ActivityMethod
    void cancelTask(String claimReference, String taskId, String reason);

    @ActivityMethod
    void recordCompletion(String claimReference);
}
