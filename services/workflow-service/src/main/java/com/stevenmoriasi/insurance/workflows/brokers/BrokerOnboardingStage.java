package com.stevenmoriasi.insurance.workflows.brokers;

public enum BrokerOnboardingStage {
    AWAITING_DOCUMENTS,
    AWAITING_AUTOMATED_CHECK,
    AWAITING_MANUAL_REVIEW,
    AWAITING_APPROVAL,
    COMPLETED,
    REJECTED,
    CANCELLED
}
