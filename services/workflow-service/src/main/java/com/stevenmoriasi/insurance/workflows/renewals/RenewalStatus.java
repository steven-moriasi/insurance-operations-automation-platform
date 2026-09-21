package com.stevenmoriasi.insurance.workflows.renewals;

import com.stevenmoriasi.insurance.workflows.shared.ExceptionRoute;
import com.stevenmoriasi.insurance.workflows.shared.HumanTaskView;
import java.util.List;

public record RenewalStatus(
        RenewalWorkflowStage stage,
        HumanTaskView currentTask,
        List<ExceptionRoute> exceptionRoutes,
        String cancellationReason) {}
