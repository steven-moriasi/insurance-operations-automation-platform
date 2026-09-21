package com.stevenmoriasi.insurance.workflows.claims;

public record MotorClaimInput(
        String claimReference,
        long evidenceSlaSeconds,
        long assessmentSlaSeconds,
        long decisionSlaSeconds,
        long settlementSlaSeconds) {}
