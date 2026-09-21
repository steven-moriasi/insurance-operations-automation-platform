package com.stevenmoriasi.insurance.workflows.brokers;

import com.stevenmoriasi.insurance.workflows.shared.ExceptionRoute;
import com.stevenmoriasi.insurance.workflows.shared.HumanTaskView;
import java.util.List;

public record BrokerOnboardingStatus(
        BrokerOnboardingStage stage,
        HumanTaskView currentTask,
        List<ExceptionRoute> exceptionRoutes,
        String cancellationReason) {}
