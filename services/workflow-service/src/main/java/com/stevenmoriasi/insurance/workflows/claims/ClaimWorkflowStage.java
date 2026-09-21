package com.stevenmoriasi.insurance.workflows.claims;

public enum ClaimWorkflowStage {
    VERIFYING_COVERAGE,
    AWAITING_EVIDENCE,
    AWAITING_ASSESSMENT,
    AWAITING_DECISION,
    AWAITING_SETTLEMENT,
    COMPLETED,
    CANCELLED
}
