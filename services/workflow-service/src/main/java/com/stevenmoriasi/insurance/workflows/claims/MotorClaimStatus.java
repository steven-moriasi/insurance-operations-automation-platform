package com.stevenmoriasi.insurance.workflows.claims;

import java.util.List;

public record MotorClaimStatus(
        ClaimWorkflowStage stage,
        List<String> overdueStages,
        String currentTaskId,
        String cancellationReason) {}
