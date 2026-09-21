package com.stevenmoriasi.insurance.workflows.renewals;

import com.stevenmoriasi.insurance.workflows.shared.ExceptionRoute;
import java.util.List;

public record RenewalResult(
        String policyNumber, RenewalWorkflowStage outcome, List<ExceptionRoute> exceptionRoutes) {}
