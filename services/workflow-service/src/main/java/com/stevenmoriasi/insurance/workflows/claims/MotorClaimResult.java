package com.stevenmoriasi.insurance.workflows.claims;

import java.util.List;

public record MotorClaimResult(
        String claimReference, ClaimWorkflowStage outcome, List<String> overdueStages) {}
