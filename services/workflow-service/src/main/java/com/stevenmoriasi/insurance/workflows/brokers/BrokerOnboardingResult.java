package com.stevenmoriasi.insurance.workflows.brokers;

import com.stevenmoriasi.insurance.workflows.shared.ExceptionRoute;
import java.util.List;

public record BrokerOnboardingResult(
        String brokerReference,
        BrokerOnboardingStage outcome,
        List<ExceptionRoute> exceptionRoutes) {}
